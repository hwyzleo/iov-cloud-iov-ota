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
     * 重试事件ID（业务消息身份幂等键，CR-026）
     */
    @TableField("retry_event_id")
    private String retryEventId;

    /**
     * 车辆任务ID（CR-026）
     */
    @TableField("vehicle_task_id")
    private Long vehicleTaskId;

    /**
     * 阶段：DOWNLOAD/INSTALL
     */
    @TableField("stage")
    private String stage;

    /**
     * 动作：RETRY/RESUME/RESET_OFFSET/EXECUTION_CREATED（CR-026）
     */
    @TableField("action")
    private String action;

    /**
     * 错误码（CR-026）
     */
    @TableField("error_code")
    private String errorCode;

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
     * 车端发生时间（未知为 null，CR-026）
     */
    @TableField("source_occurred_at")
    private LocalDateTime sourceOccurredAt;

    /**
     * 云端受理时间（CR-026）
     */
    @TableField("received_at")
    private LocalDateTime receivedAt;

    /**
     * 备注
     */
    @TableField("description")
    private String description;
}
