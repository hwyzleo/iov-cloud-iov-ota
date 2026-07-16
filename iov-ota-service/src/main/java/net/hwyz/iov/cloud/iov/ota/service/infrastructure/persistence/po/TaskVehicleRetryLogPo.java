package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 重试/续传轨迹审计持久化对象
 * 对应表：tb_task_vehicle_retry_log
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_retry_log")
public class TaskVehicleRetryLogPo extends BasePo {

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
     * VIN
     */
    @TableField("vin")
    private String vin;

    /**
     * 阶段：DOWNLOAD/INSTALL
     */
    @TableField("stage")
    private String stage;

    /**
     * 尝试次数
     */
    @TableField("attempt_no")
    private Integer attemptNo;

    /**
     * 偏移量（字节）
     */
    @TableField("offset")
    private Long offset;

    /**
     * 结果：SUCCESS/FAIL
     */
    @TableField("result")
    private String result;

    /**
     * 原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 重试时间
     */
    @TableField("retried_at")
    private LocalDateTime retriedAt;

    /**
     * 备注
     */
    @TableField("description")
    private String description;
}
