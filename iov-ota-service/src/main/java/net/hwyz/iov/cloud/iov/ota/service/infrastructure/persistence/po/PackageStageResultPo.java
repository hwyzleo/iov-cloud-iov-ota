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
 * 包阶段结果 PO（CR-012 §3、§5.4）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_package_stage_result")
public class PackageStageResultPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("stage_result_id")
    private String stageResultId;

    @TableField("vehicle_task_id")
    private Long vehicleTaskId;

    @TableField("package_id")
    private String packageId;

    @TableField("stage")
    private String stage;

    @TableField("result_status")
    private String resultStatus;

    @TableField("package_revision")
    private String packageRevision;

    @TableField("etag")
    private String etag;

    @TableField("digest")
    private String digest;

    @TableField("signature_result")
    private String signatureResult;

    @TableField("decrypt_result")
    private String decryptResult;

    @TableField("fail_reason")
    private String failReason;

    /** 下载尝试序号（历史数据未知为 null，CR-026） */
    @TableField("download_attempt_no")
    private Integer downloadAttemptNo;

    /** 车端发生时间（未知为 null，CR-026） */
    @TableField("source_occurred_at")
    private Date sourceOccurredAt;

    /** 云端受理时间（历史数据未知为 null，CR-026） */
    @TableField("received_at")
    private Date receivedAt;
}
