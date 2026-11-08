package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车辆关联活动查询结果（IOV-OTA-DSN-CR-023 §4.1）
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleActivityRelationResult {

    private Long activityId;

    private String activityName;

    /** tb_activity.state 原始状态码，由装配层转换为 ActivityState 枚举名 */
    private Integer activityState;

    private Long vehicleTaskCount;

    private java.util.Date latestTaskTime;
}
