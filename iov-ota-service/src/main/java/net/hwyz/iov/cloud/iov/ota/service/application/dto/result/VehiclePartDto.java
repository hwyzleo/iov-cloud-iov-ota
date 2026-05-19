package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 车辆零件结果DTO
 */
@Data
@Builder
public class VehiclePartDto {
    private Long id;
    private String vehicleModelCode;
    private String partCode;
    private String partPn;
    private String partName;
    private String partType;
    private String description;
    private Integer status;
    private Instant createTime;
}
