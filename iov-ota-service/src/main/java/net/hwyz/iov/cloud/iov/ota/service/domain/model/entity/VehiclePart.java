package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 车辆零件实体
 */
@Data
@Builder
public class VehiclePart implements Serializable {
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
