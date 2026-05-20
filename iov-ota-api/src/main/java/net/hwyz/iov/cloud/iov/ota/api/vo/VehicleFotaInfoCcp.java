package net.hwyz.iov.cloud.iov.ota.api.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 中央计算平台车端在线固件升级信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleFotaInfoCcp {

    /**
     * 基线
     */
    @NotBlank(message = "基线不能为空")
    private String baseline;

    /**
     * 设备信息列表
     */
    @NotEmpty(message = "设备信息列表不能为空")
    private List<DeviceInfoCcp> deviceInfoList;

}
