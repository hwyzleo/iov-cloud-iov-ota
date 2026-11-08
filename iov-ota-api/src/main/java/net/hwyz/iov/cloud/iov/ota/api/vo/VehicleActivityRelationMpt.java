package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 车辆关联升级活动（IOV-OTA-DSN-CR-023 §4.1）
 *
 * <p>以已生成的 VehicleTask 为关联依据，同一活动在该 VIN 下仅返回一条记录。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleActivityRelationMpt {

    /** 升级活动ID */
    private Long activityId;

    /** 活动名称 */
    private String activityName;

    /** 活动状态（ActivityState 枚举名） */
    private String activityState;

    /** 该 VIN 在此活动下已生成的 VehicleTask 数量 */
    private Long vehicleTaskCount;

    /** 该 VIN 在此活动下最近一条 VehicleTask 创建时间 */
    private Date latestTaskTime;
}
