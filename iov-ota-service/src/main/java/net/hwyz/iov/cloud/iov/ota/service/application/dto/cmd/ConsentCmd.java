package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户授权命令（CR-016 §5/§4.20.3）
 *
 * <p>由 ConsentCommandHandler 从 vehicle.fota.v1.ConsentReport + Envelope 元数据构建。
 * 授权写入只来自正式车云业务消息；管理后台只读。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentCmd {

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 任务ID */
    private Long taskId;

    /** 车架号 */
    private String vin;

    /** 授权动作：GRANT / DENY / REVOKE */
    private String action;

    /** 消息所针对的任务修订 */
    private Long taskRevision;

    /** 条款文章ID */
    private Long articleId;

    /** 条款展示版本 */
    private String articleVersion;

    /** 条款权威摘要 */
    private String articleHash;

    /** 授权范围摘要（由云端按冻结快照计算/校验） */
    private String consentScopeDigest;

    /** 授权回执ID（GRANTED 时生成稳定 ID） */
    private String consentReceiptId;

    /** 上报渠道：HMI/TBOX/APP */
    private String channel;

    /** 授权主体引用 */
    private String subjectRef;

    /** 车端业务时间 */
    private Instant reportedAt;

    /** 可选有效期 */
    private Instant expireAt;

    /** Kafka Envelope 消息ID */
    private String messageId;

    /** 写入幂等键 */
    private String idempotencyKey;

    /** 请求摘要（同键异参冲突检测） */
    private String requestDigest;

    /** GRANTED 后是否需要下载（默认 true，决定车辆任务状态推进） */
    private Boolean downloadRequired;
}
