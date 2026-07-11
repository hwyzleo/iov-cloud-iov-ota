package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.Date;

/**
 * tb_task_batch 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_batch")
public class TaskBatchPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 升级任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 阶段
     */
    @TableField("phase")
    private String phase;

    /**
     * 批次号
     */
    @TableField("batch_no")
    private Integer batchNo;

    /**
     * 放量比例
     */
    @TableField("ratio")
    private java.math.BigDecimal ratio;

    /**
     * 目标表达式
     */
    @TableField("target_expr")
    private String targetExpr;

    /**
     * 批次状态
     */
    @TableField("state")
    private String state;

    /**
     * 放量时间
     */
    @TableField("released_at")
    private Date releasedAt;

}
