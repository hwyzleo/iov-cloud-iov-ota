package net.hwyz.iov.cloud.iov.ota.api.contract;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 中央计算平台设备信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoCcp {

    /**
     * 设备代码
     */
    @NotBlank(message = "设备代码不能为空")
    private String deviceCode;

    /**
     * 零件序列号
     */
    private String partSn;

    /**
     * 零件号（总成号）
     */
    @NotBlank(message = "零件号不能为空")
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
    @NotBlank(message = "软件零件号不能为空")
    private String softwarePn;

    /**
     * 软件零件版本号
     */
    @NotBlank(message = "软件零件版本号不能为空")
    private String softwarePartVer;

    /**
     * 配置字
     */
    private String configWord;

    /**
     * 额外信息
     */
    private Map<String, String> extra;

}
