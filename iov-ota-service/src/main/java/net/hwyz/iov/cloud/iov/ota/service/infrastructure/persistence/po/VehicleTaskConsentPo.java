package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.util.Date;

/**
 * 车辆任务授权唯一事实表 PO（CR-016 §3.2）
 *
 * <p>追加历史记录，不覆盖此前同意/拒绝/撤回事实；当前状态由 tb_task_vehicle 强一致保存。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_task_consent")
public class VehicleTaskConsentPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("vehicle_task_id")
    private Long vehicleTaskId;

    @TableField("task_id")
    private Long taskId;

    @TableField("vin")
    private String vin;

    @TableField("task_revision")
    private Long taskRevision;

    @TableField("consent_result")
    private String consentResult;

    @TableField("consent_receipt_id")
    private String consentReceiptId;

    @TableField("supersedes_consent_id")
    private Long supersedesConsentId;

    @TableField("article_id")
    private Long articleId;

    @TableField("article_version")
    private String articleVersion;

    @TableField("article_hash")
    private String articleHash;

    @TableField("consent_scope_digest")
    private String consentScopeDigest;

    @TableField("channel")
    private String channel;

    @TableField("subject_ref")
    private String subjectRef;

    @TableField("reported_at")
    private Date reportedAt;

    @TableField("received_at")
    private Date receivedAt;

    @TableField("expire_at")
    private Date expireAt;

    @TableField("message_id")
    private String messageId;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("request_digest")
    private String requestDigest;

    @TableField("source_model")
    private String sourceModel;
}
