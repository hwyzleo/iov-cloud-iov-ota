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
 * tb_task_metric 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_metric")
public class TaskMetricPo extends BasePo {

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
     * 批次号
     */
    @TableField("batch_no")
    private Integer batchNo;

    /**
     * 成功数
     */
    @TableField("success_cnt")
    private Integer successCnt;

    /**
     * 失败数
     */
    @TableField("fail_cnt")
    private Integer failCnt;

    /**
     * 超时数
     */
    @TableField("timeout_cnt")
    private Integer timeoutCnt;

    /**
     * 失败率
     */
    @TableField("fail_rate")
    private java.math.BigDecimal failRate;

    /**
     * 门禁阈值
     */
    @TableField("gate_threshold")
    private java.math.BigDecimal gateThreshold;

    /**
     * 门禁状态
     */
    @TableField("gate_state")
    private String gateState;

    /**
     * 统计时间
     */
    @TableField("stat_time")
    private Date statTime;

}
