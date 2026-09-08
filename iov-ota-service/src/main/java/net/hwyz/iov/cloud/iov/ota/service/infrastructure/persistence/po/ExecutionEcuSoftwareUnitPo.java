package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * 安装执行 ECU 软件单元结果子表 PO（CR-019 §4.23.5）
 *
 * <p>ECU 行保存聚合结果（tb_task_vehicle_execution_ecu_result），per-Target/Slot
 * 单元结果落本子表（SoftwareUnit 结构），与 inventory SoftwareUnit 对称。
 * SINGLE_IMAGE 写一条 Target=ECU_IMAGE。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_execution_ecu_software_unit")
public class ExecutionEcuSoftwareUnitPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("ecu_result_id")
    private Long ecuResultId;

    @TableField("execution_id")
    private Long executionId;

    @TableField("ecu_id")
    private String ecuId;

    /** 软件目标编码 */
    @TableField("software_target_code")
    private String softwareTargetCode;

    @TableField("source_version")
    private String sourceVersion;

    @TableField("target_version")
    private String targetVersion;

    @TableField("actual_version")
    private String actualVersion;

    @TableField("slot")
    private String slot;

    @TableField("active")
    private Boolean active;

    /** 结果：SUCCESS/FAILED/ROLLED_BACK */
    @TableField("result")
    private String result;

    @TableField("failure_stage")
    private String failureStage;

    @TableField("rollback_result")
    private String rollbackResult;

    @TableField("package_id")
    private String packageId;
}
