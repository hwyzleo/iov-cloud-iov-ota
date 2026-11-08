package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 车辆关联任务查询条件（IOV-OTA-DSN-CR-023 §4.2）
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRelationTaskQuery {

    /** 车架号 */
    private String vin;

    /** 升级活动ID（可选） */
    private Long activityId;

    /** 任务状态码 TaskState.value（可选） */
    private Integer taskState;

    /** VehicleTask 状态（可选） */
    private String vehicleTaskStatus;

    /** VehicleTask 创建时间下界（含） */
    private Date beginTime;

    /** VehicleTask 创建时间上界（含） */
    private Date endTime;
}
