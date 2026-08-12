package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * 安装执行 ECU 结果 PO（CR-012 §3、§5.7）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_execution_ecu_result")
public class ExecutionEcuResultPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("execution_id")
    private Long executionId;

    @TableField("ecu_id")
    private String ecuId;

    @TableField("target_software_version")
    private String targetSoftwareVersion;

    @TableField("actual_software_version")
    private String actualSoftwareVersion;

    @TableField("result")
    private String result;

    @TableField("fail_reason")
    private String failReason;
}
