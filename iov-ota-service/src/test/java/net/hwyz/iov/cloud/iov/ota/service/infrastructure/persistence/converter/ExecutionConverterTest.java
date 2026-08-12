package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.OtaExecutionPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionConverter 转换器测试（CR-012）
 *
 * @author hwyz_leo
 */
@DisplayName("ExecutionConverter PO <-> Domain 转换")
class ExecutionConverterTest {

    private final ExecutionConverter converter = new ExecutionConverter();

    @Test
    @DisplayName("toPo 应正确映射聚合字段到 PO")
    void toPo_shouldMapAggregateFields() {
        Instant validUntil = Instant.now().plusSeconds(3600);
        Execution execution = Execution.permit(
                ExecutionId.of(1L), VehicleTaskId.of(10L), 2,
                TaskRevision.of(3L), "PLAN_V1",
                SnapshotDigest.of("manifestDigest"), "COND_V1",
                PermitToken.of("permit-token", validUntil), validUntil);
        execution.startInstall(Instant.now());
        execution.receiveEvent(1);
        execution.receiveEvent(2);

        OtaExecutionPo po = converter.toPo(execution);

        assertEquals(1L, po.getId());
        assertEquals(10L, po.getVehicleTaskId());
        assertEquals(2, po.getAttemptNo());
        assertEquals(ExecutionStatus.INSTALLING.getValue(), po.getStatus());
        assertEquals(3L, po.getTaskRevision());
        assertEquals("PLAN_V1", po.getInstallPlanVersion());
        assertEquals("manifestDigest", po.getPackageManifestDigest());
        assertEquals("COND_V1", po.getConditionSetVersion());
        assertEquals("permit-token", po.getPermitToken());
        assertEquals(2L, po.getAcceptedSequenceNo());
    }

    @Test
    @DisplayName("toDomain 应正确重建聚合（含水位）")
    void toDomain_shouldReconstituteWithWatermark() {
        OtaExecutionPo po = OtaExecutionPo.builder()
                .id(1L)
                .executionId("1")
                .vehicleTaskId(10L)
                .attemptNo(2)
                .status(ExecutionStatus.INSTALLING.getValue())
                .taskRevision(3L)
                .installPlanVersion("PLAN_V1")
                .packageManifestDigest("manifestDigest")
                .conditionSetVersion("COND_V1")
                .permitToken("permit-token")
                .acceptedSequenceNo(5L)
                .finalSequenceNo(10L)
                .build();

        Execution execution = converter.toDomain(po);

        assertNotNull(execution);
        assertEquals(1L, execution.getId().getValue());
        assertEquals(10L, execution.getVehicleTaskId().getValue());
        assertEquals(2, execution.getAttemptNo());
        assertEquals(ExecutionStatus.INSTALLING, execution.getStatus());
        assertEquals(3L, execution.getTaskRevision().getValue());
        assertEquals("PLAN_V1", execution.getInstallPlanVersion());
        assertEquals(5L, execution.getSequenceWatermark().getAcceptedSequenceNo());
        assertEquals(10L, execution.getFinalSequenceNo());
        assertTrue(execution.getPendingEvents().isEmpty());
    }

    @Test
    @DisplayName("toDomain null PO 返回 null")
    void toDomain_nullPo_returnsNull() {
        assertNull(converter.toDomain(null));
    }

    @Test
    @DisplayName("toPo/toDomain 往返保持核心字段一致")
    void roundTrip_shouldPreserveCoreFields() {
        Instant validUntil = Instant.now().plusSeconds(1800);
        Execution original = Execution.permit(
                ExecutionId.of(3L), VehicleTaskId.of(30L), 1,
                TaskRevision.of(1L), "PLAN_V2",
                SnapshotDigest.of("digest"), "COND_V2",
                PermitToken.of("tok", validUntil), validUntil);
        original.startInstall(Instant.now());
        original.defineFinalSequenceNo(5);
        original.receiveEvent(1);
        original.receiveEvent(2);
        original.receiveEvent(3);

        OtaExecutionPo po = converter.toPo(original);
        Execution restored = converter.toDomain(po);

        assertEquals(original.getId().getValue(), restored.getId().getValue());
        assertEquals(original.getVehicleTaskId().getValue(), restored.getVehicleTaskId().getValue());
        assertEquals(original.getAttemptNo(), restored.getAttemptNo());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(original.getTaskRevision().getValue(), restored.getTaskRevision().getValue());
        assertEquals(original.getInstallPlanVersion(), restored.getInstallPlanVersion());
        assertEquals(original.getSequenceWatermark().getAcceptedSequenceNo(),
                restored.getSequenceWatermark().getAcceptedSequenceNo());
        assertEquals(original.getFinalSequenceNo(), restored.getFinalSequenceNo());
    }
}
