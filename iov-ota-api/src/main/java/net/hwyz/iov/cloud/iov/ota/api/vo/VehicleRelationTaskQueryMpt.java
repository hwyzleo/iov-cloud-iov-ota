package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 车辆关联升级任务查询参数（IOV-OTA-DSN-CR-023 §4.2）
 *
 * <p>时间范围沿用 BaseRequest.params 约定（params.beginTime / params.endTime），
 * 过滤维度为 VehicleTask 创建时间。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleRelationTaskQueryMpt extends BaseRequest {

    /** 升级活动ID（可选） */
    private Long activityId;

    /** 任务状态码 TaskState.value（可选） */
    private Integer taskState;

    /** VehicleTask 状态 VehicleTaskStatus.name()（可选） */
    private String vehicleTaskStatus;
}
