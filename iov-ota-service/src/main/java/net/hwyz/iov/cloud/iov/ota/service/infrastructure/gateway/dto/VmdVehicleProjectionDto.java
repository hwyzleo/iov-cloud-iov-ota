package net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VMD 车辆主档查询结果 DTO（CR-015 §4.1）
 * <p>对应 GET /api/service/vehicle/v1/{vin}；字段与 tb_vehicle_projection 投影所需最小集合对齐。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdVehicleProjectionDto {

    /** 车架号 */
    private String vin;

    /** 生产时间（ISO-8601） */
    private String productionTime;

    private String plantCode;
    private String brandCode;
    private String platformCode;
    private String carLineCode;
    private String modelCode;
    private String variantCode;
    private String configurationCode;

    /** 上游数据版本（用于幂等与乱序判断） */
    private Long sourceVersion;
}
