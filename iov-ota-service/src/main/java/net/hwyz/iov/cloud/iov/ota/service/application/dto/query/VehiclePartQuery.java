package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;

/**
 * 车辆零件查询条件
 */
@Data
public class VehiclePartQuery {
    private String vehicleModelCode;
    private String partCode;
    private String partPn;
    private String partName;
    private Integer status;
}
