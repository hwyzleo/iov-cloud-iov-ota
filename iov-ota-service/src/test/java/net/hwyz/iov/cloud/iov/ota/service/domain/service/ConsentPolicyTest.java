package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConsentPolicy 统一授权有效性判定测试（CR-016 §4）
 *
 * @author hwyz_leo
 */
@DisplayName("ConsentPolicy 授权有效性判定")
class ConsentPolicyTest {

    private final ConsentPolicy policy = new ConsentPolicy();
    private final Instant now = Instant.now();

    private VehicleTask vehicleTask;
    private VehicleTaskConsent current;

    @BeforeEach
    void setUp() {
        vehicleTask = VehicleTask.create(
                VehicleTaskId.of(1L), 100L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                now.minusSeconds(60), now.plusSeconds(60), now.plusSeconds(3600),
                true, 200L, "v1", "terms-hash");
        vehicleTask.markVisible(now);
        vehicleTask.enterConsentPending();
        vehicleTask.applyConsent(ConsentResult.GRANTED, 5L, "scope-1", now, false);

        current = new VehicleTaskConsent()
                .setId(5L)
                .setVehicleTaskId(1L)
                .setTaskId(100L)
                .setVin("VIN001")
                .setTaskRevision(vehicleTask.getTaskRevision().getValue())
                .setResult(ConsentResult.GRANTED)
                .setConsentReceiptId("RCPT-1")
                .setArticleId(200L)
                .setArticleVersion("v1")
                .setArticleHash("terms-hash")
                .setConsentScopeDigest("scope-1");
    }

    @Test
    @DisplayName("GRANTED + 绑定一致 + 未过期 -> 有效")
    void granted_withMatchingBinding_isPermitted() {
        assertTrue(policy.isPermitted(vehicleTask, current, now));
        assertNull(policy.invalidReason(vehicleTask, current, now));
    }

    @Test
    @DisplayName("consentRequired=false 且非 GRANTED 也有效")
    void notRequired_isPermittedRegardlessOfState() {
        VehicleTask noConsent = VehicleTask.create(
                VehicleTaskId.of(2L), 100L, "VIN002",
                TaskRevision.initial(), SnapshotDigest.of("d"),
                now.minusSeconds(60), now.plusSeconds(60), now.plusSeconds(3600),
                false, null, null, null);
        assertTrue(policy.isPermitted(noConsent, null, now));
    }

    @Test
    @DisplayName("授权状态非 GRANTED -> 无效")
    void notGranted_isInvalid() {
        vehicleTask.setConsentState(ConsentState.REVOKED);
        assertFalse(policy.isPermitted(vehicleTask, current, now));
        assertNotNull(policy.invalidReason(vehicleTask, current, now));
    }

    @Test
    @DisplayName("缺少当前授权记录 -> 无效")
    void missingCurrentConsent_isInvalid() {
        assertFalse(policy.isPermitted(vehicleTask, null, now));
    }

    @Test
    @DisplayName("授权记录与车辆任务不匹配 -> 无效")
    void mismatchedVehicleTask_isInvalid() {
        current.setVehicleTaskId(999L);
        assertFalse(policy.isPermitted(vehicleTask, current, now));
    }

    @Test
    @DisplayName("授权记录任务修订与当前不一致 -> 无效")
    void mismatchedTaskRevision_isInvalid() {
        current.setTaskRevision(9L);
        assertFalse(policy.isPermitted(vehicleTask, current, now));
    }

    @Test
    @DisplayName("授权范围摘要不一致 -> 无效")
    void mismatchedScopeDigest_isInvalid() {
        current.setConsentScopeDigest("other-scope");
        assertFalse(policy.isPermitted(vehicleTask, current, now));
    }

    @Test
    @DisplayName("授权条款摘要与冻结条款不一致 -> 无效")
    void mismatchedArticleHash_isInvalid() {
        current.setArticleHash("other-hash");
        assertFalse(policy.isPermitted(vehicleTask, current, now));
    }

    @Test
    @DisplayName("授权已过期 -> 无效")
    void expired_isInvalid() {
        current.setExpireAt(now.minusSeconds(1));
        assertFalse(policy.isPermitted(vehicleTask, current, now));
    }
}
