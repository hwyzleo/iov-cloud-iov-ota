package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.util.List;
import java.util.Map;

/**
 * 车辆领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@SuperBuilder
public class VehicleDo extends BaseDo<String> implements DomainObj<VehicleDo> {

    /**
     * 基线代码
     */
    private String baselineCode;

    /**
     * 是否基线对齐
     */
    private Boolean isBaselineAlignment;

    /**
     * 设备信息Map
     */
    private Map<String, DeviceInfoVo> deviceMap;

    /**
     * 初始化
     */
    public void init() {
        stateInit();
    }

    /**
     * 校验基线是否变更
     *
     * @param baselineCode 基线代码
     * @return true: 变更，false: 不变更
     */
    public boolean checkBaseline(String baselineCode) {
        if (this.baselineCode == null || !this.baselineCode.equals(baselineCode)) {
            this.baselineCode = baselineCode;
            stateChange();
            return true;
        }
        return false;
    }

    /**
     * 校验设备信息是否变更
     *
     * @param deviceInfoList 设备信息列表
     * @return true: 变更，false: 不变更
     */
    public boolean checkDevices(List<DeviceInfoVo> deviceInfoList) {
        boolean flag = false;
        if (this.deviceMap == null || this.deviceMap.isEmpty() || this.deviceMap.size() != deviceInfoList.size()) {
            flag = true;
        } else {
            for (DeviceInfoVo deviceInfoNew : deviceInfoList) {
                DeviceInfoVo deviceInfoOld = this.deviceMap.get(deviceInfoNew.getDeviceCode());
                if (!deviceInfoNew.equals(deviceInfoOld)) {
                    flag = true;
                    break;
                }
            }
        }
        if (flag) {
            this.deviceMap = deviceInfoList.stream().collect(java.util.stream.Collectors.toMap(DeviceInfoVo::getDeviceCode, deviceInfo -> deviceInfo));
            stateChange();
        }
        return flag;
    }

    /**
     * 标记基线是否对齐
     *
     * @param isBaselineAlignment 基线是否对齐
     */
    public void markBaselineAlignment(Boolean isBaselineAlignment) {
        this.isBaselineAlignment = isBaselineAlignment;
        stateChange();
    }

}
