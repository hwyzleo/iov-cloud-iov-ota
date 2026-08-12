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
 * 安装执行主表 PO（CR-012 §3）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_execution")
public class OtaExecutionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("execution_id")
    private String executionId;

    @TableField("vehicle_task_id")
    private Long vehicleTaskId;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("status")
    private String status;

    @TableField("task_revision")
    private Long taskRevision;

    @TableField("install_plan_version")
    private String installPlanVersion;

    @TableField("package_manifest_digest")
    private String packageManifestDigest;

    @TableField("condition_set_version")
    private String conditionSetVersion;

    @TableField("permit_token")
    private String permitToken;

    @TableField("offline_policy")
    private String offlinePolicy;

    @TableField("timeout_policy")
    private String timeoutPolicy;

    @TableField("control_policy")
    private String controlPolicy;

    @TableField("valid_until")
    private Date validUntil;

    @TableField("accepted_sequence_no")
    private Long acceptedSequenceNo;

    @TableField("final_sequence_no")
    private Long finalSequenceNo;

    @TableField("start_time")
    private Date startTime;

    @TableField("end_time")
    private Date endTime;
}
