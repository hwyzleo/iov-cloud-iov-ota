package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult;

import java.time.Instant;

/**
 * VehicleTask 授权历史记录领域实体（CR-016 §2/§3.2）
 *
 * <p>Consent 是 VehicleTask 的子实体：追加保存同意、拒绝、撤回与重新授权事实，
 * 不覆盖历史。当前权威状态由 VehicleTask（tb_task_vehicle）强一致保存。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class VehicleTaskConsent {

    private Long id;
    private Long vehicleTaskId;
    private Long taskId;
    private String vin;
    private Long taskRevision;
    private ConsentResult result;
    private String consentReceiptId;
    private Long supersedesConsentId;
    private Long articleId;
    private String articleVersion;
    private String articleHash;
    private String consentScopeDigest;
    private String channel;
    private String subjectRef;
    private Instant reportedAt;
    private Instant receivedAt;
    private Instant expireAt;
    private String messageId;
    private String idempotencyKey;
    private String requestDigest;
    private String sourceModel;

    /**
     * 是否为可执行授权（GRANTED）。
     */
    public boolean isGranted() {
        return result == ConsentResult.GRANTED;
    }
}
