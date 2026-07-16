package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 任务实例化安装条件持久化对象
 * 对应表：tb_task_install_condition
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_install_condition")
public class TaskInstallConditionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 条件类型编码
     */
    @TableField("condition_type")
    private String conditionType;

    /**
     * 操作符：EQ/NE/GT/GE/LT/LE/IN/NOT_IN
     */
    @TableField("operator")
    private String operator;

    /**
     * 阈值
     */
    @TableField("threshold")
    private String threshold;

    /**
     * 严重级别：BLOCK/WARN
     */
    @TableField("severity")
    private String severity;

    /**
     * 备注
     */
    @TableField("description")
    private String description;
}
