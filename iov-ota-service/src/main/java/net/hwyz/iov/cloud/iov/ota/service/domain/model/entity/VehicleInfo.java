package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VehicleInfo {
    
    private final String vin;
    private final String baselineCode;
    private final Boolean isBaselineAlignment;
}