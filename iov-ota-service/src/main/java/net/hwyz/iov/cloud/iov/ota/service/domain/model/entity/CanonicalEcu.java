package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 规范 ECU 实体（CR-019 §4.1）
 *
 * <p>一个 ECU 实例的硬件身份与全部规范软件单元；硬件零件号不得作为
 * Software Target 标识。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class CanonicalEcu {

    /** ECU / VehicleNode 标识 */
    private final String ecuId;
    /** 硬件零件号 */
    private final String hardwarePartNumber;
    /** 硬件版本 */
    private final String hwVersion;
    /** 规范软件单元列表（SINGLE_IMAGE 时恰含一个 ECU_IMAGE 单元） */
    private final List<net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit> softwareUnits;
}
