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
 * tb_task_report 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_report")
public class TaskReportPo extends BasePo {

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
     * 完成率
     */
    @TableField("complete_rate")
    private java.math.BigDecimal completeRate;

    /**
     * 成功率
     */
    @TableField("success_rate")
    private java.math.BigDecimal successRate;

    /**
     * 失败case分布（JSON）
     */
    @TableField("fail_case_dist")
    private String failCaseDist;

    /**
     * 生成时间
     */
    @TableField("gen_time")
    private Date genTime;

}
