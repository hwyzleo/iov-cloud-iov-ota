package net.hwyz.iov.cloud.iov.ota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 中央计算平台车辆升级任务过程
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskVehicleProcessCcp {

    /**
     * 升级任务车辆主键
     */
    private Long id;

    /**
     * 升级任务ID
     */
    private Long taskId;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 升级设备
     */
    private String device;

    /**
     * 设备序列号
     */
    private String deviceSn;

    /**
     * 执行操作
     */
    private Short operation;

    /**
     * 操作时间
     */
    private Date operationTime;

    /**
     * 操作结果
     */
    private Short operationResult;

    /**
     * 重试次数
     */
    private Short retryCount;

    /**
     * 异常代码
     */
    private Short errorCode;

    /**
     * 异常消息
     */
    private String errorMsg;

    /**
     * 扩展内容
     */
    private String extra;

}
