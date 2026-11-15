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
     * 是否当前车辆任务（IOV-OTA-DSN-CR-025 §4.29.6）：同一 (task_id, vin) 至多一条当前行
     */
    private Boolean isCurrent;

    /**
     * 取代者车辆任务ID（仅被取代的历史行为非空）
     */
    private Long supersededBy;

    /**
     * 该车该任务被重置次数（来自重置审计聚合）
     */
    private Integer resetCount;

    /**
     * 创建时间
     */
    private Date createTime;

}
