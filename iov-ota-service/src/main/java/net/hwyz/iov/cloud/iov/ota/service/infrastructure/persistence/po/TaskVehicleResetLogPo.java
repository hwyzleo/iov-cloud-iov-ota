package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * 车辆任务重置审计持久化对象（IOV-OTA-DSN-CR-025 §4.29.4）。
 *
 * <p>对应表 tb_task_vehicle_reset_log：每次重置追加一行，记录原/新车辆任务、
 * 原运行态、被强制收口的活动执行、是否强制、原因与操作人；不可变追加，
 * 不随重置更新旧事实。</p>
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_reset_log")
public class TaskVehicleResetLogPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 原车辆任务ID（已置 SUPERSEDED） */
    @TableField("old_vehicle_task_id")
    private Long oldVehicleTaskId;

    /** 新车辆任务ID（初始化状态） */
    @TableField("new_vehicle_task_id")
    private Long newVehicleTaskId;

    /** 升级任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 升级活动ID */
    @TableField("activity_id")
    private Long activityId;

    /** 车架号 */
    @TableField("vin")
    private String vin;

    /** 重置前车辆任务状态 */
    @TableField("old_status")
    private String oldStatus;

    /** 重置前下载准备状态 */
    @TableField("old_download_ready")
    private String oldDownloadReady;

    /** 重置前授权状态 */
    @TableField("old_consent_state")
    private String oldConsentState;

    /** 重置前尝试序号 */
    @TableField("old_attempt_no")
    private Integer oldAttemptNo;

    /** 被强制收口的活动执行ID */
    @TableField("old_execution_id")
    private Long oldExecutionId;

    /** 被强制收口前执行状态 */
    @TableField("old_execution_status")
    private String oldExecutionStatus;

    /** 是否强制执行中重置 */
    @TableField("force_flag")
    private Integer forceFlag;

    /** 重置原因 */
    @TableField("reason")
    private String reason;

    /** 结果：SUCCESS/FAILED */
    @TableField("result")
    private String result;
}
