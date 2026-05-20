package net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Objects;

/**
 * 设备信息值对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoVo {

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 零件序列号
     */
    private String partSn;

    /**
     * 零件号（总成号）
     */
    private String partNo;

    /**
     * 硬件零件号
     */
    private String hardwarePn;

    /**
     * 硬件零件版本号
     */
    private String hardwarePartVer;

    /**
     * 软件零件号
     */
    private String softwarePn;

    /**
     * 软件零件版本号
     */
    private String softwarePartVer;

    /**
     * 软件内部版本
     */
    private String softwareBuildVer;

    /**
     * 配置字
     */
    private String configWord;

    /**
     * 额外信息
     */
    private Map<String, String> extra;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceInfoVo deviceInfoVo = (DeviceInfoVo) o;
        return Objects.equals(deviceCode, deviceInfoVo.deviceCode) && Objects.equals(partSn, deviceInfoVo.partSn) &&
                Objects.equals(partNo, deviceInfoVo.partNo) && Objects.equals(hardwarePn, deviceInfoVo.hardwarePn) &&
                Objects.equals(hardwarePartVer, deviceInfoVo.hardwarePartVer) && Objects.equals(softwarePn, deviceInfoVo.softwarePn)
                && Objects.equals(softwarePartVer, deviceInfoVo.softwarePartVer) && Objects.equals(configWord, deviceInfoVo.configWord)
                && Objects.equals(extra, deviceInfoVo.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceCode, partSn, partNo, hardwarePn, hardwarePartVer, softwarePn, softwarePartVer, configWord, extra);
    }
}
