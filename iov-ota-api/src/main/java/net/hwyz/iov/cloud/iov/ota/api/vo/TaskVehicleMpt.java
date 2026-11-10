package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车辆升级任务
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskVehicleMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 升级活动ID
     */
    private Long activityId;

    /**
     * 升级活动名称
     */
    private String activityName;

    /**
     * 升级任务ID
     */
    private Long taskId;

    /**
     * 升级任务名称
     */
    private String taskName;

    /**
     * 车架号
     */
    private String vin;

    /**
     * VehicleTask 状态（CR-012 状态机枚举名：CREATED/VISIBLE/CONSENT_PENDING/DOWNLOAD_PENDING/...）
     * <p>旧 tb_task_vehicle.state 数值码已由 CR-013 §6.2 删除，列表状态一律以本字段为准。
     */
    private String vehicleTaskStatus;

    /**
     * 结果代码
     */
    private String resultCode;

    /**
     * 创建时间
     */
    private Date createTime;

}
