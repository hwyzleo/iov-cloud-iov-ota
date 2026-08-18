package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 授权摘要（CR-015 §3.3 consentSummary / CR-016 §3.2）
 *
 * <p>取当前权威授权记录（tb_vehicle_task_consent）汇总；当前授权状态由
 * tb_task_vehicle.consent_state 提供。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class ConsentProcessSummary {

    /** 当前授权状态（tb_task_vehicle） */
    private String consentState;
    /** 授权历史业务结果：GRANTED/REJECTED/REVOKED */
    private String consentResult;
    /** 授权回执ID */
    private String receiptId;
    /** 任务修订 */
    private Long taskRevision;
    /** 条款文章ID */
    private Long articleId;
    /** 条款展示版本 */
    private String articleVersion;
    /** 条款权威摘要 */
    private String articleHash;
    /** 授权范围摘要 */
    private String scopeDigest;
    /** 上报渠道 */
    private String channel;
    /** 车端业务时间 */
    private Instant reportedAt;
    /** 云端接收时间 */
    private Instant receivedAt;
    /** 可选有效期 */
    private Instant expireAt;
    /** 被取代的上一条记录ID */
    private Long supersedesConsentId;
    /** 来源：NATIVE/MIGRATED_USER_CONSENT */
    private String sourceModel;
}
