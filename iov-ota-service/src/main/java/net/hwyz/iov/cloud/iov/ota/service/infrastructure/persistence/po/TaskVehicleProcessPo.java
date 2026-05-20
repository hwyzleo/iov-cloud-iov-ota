package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 升级任务车辆升级过程表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-28
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_process")
public class TaskVehicleProcessPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 升级任务车辆主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 升级任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 升级设备
     */
    @TableField("device")
    private String device;

    /**
     * 设备序列号
     */
    @TableField("device_sn")
    private String deviceSn;

    /**
     * 执行操作
     */
    @TableField("operation")
    private Integer operation;

    /**
     * 操作时间
     */
    @TableField("operation_time")
    private Date operationTime;

    /**
     * 操作结果
     */
    @TableField("operation_result")
    private Integer operationResult;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 异常代码
     */
    @TableField("error_code")
    private Integer errorCode;

    /**
     * 异常消息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 扩展内容
     */
    @TableField("extra")
    private String extra;
}
