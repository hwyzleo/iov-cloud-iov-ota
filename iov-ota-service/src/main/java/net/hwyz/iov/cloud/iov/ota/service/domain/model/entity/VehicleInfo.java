package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VehicleInfo {
    
    private final String vin;
    private final String baselineCode;
    private final Boolean isBaselineAlignment;
    
    // 扩展字段用于条件分层建模（US-058/059）
    private final String adaptationSubject;  // 适配主体（软件零件号/硬件总成号）
    private final String softwareVersion;    // 软件版本
    private final String hardwareVersion;    // 硬件版本
    private final String deviceCode;         // 设备代码
    private final String softwarePn;         // 软件零件号
    private final String hardwarePn;         // 硬件零件号
}