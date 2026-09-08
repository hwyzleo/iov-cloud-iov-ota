package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vehicle.fota.v1.Types.EcuSoftwareModel;
import vehicle.fota.v1.Types.EcuVersion;
import vehicle.fota.v1.Types.SoftwareUnitVersion;

import java.util.List;

/**
 * 清单明细项命令（CR-012 §5.1 / CR-019 §4.1）
 *
 * <p>legacy 软件单值字段仅用于 SINGLE_IMAGE 兼容读取；新业务统一读取
 * softwareUnits 规范结构。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemCmd {

    private String ecuId;
    private String ecuName;
    /** legacy SINGLE_IMAGE 软件零件号（只读兼容） */
    private String softwarePn;
    /** legacy SINGLE_IMAGE 软件版本（只读兼容） */
    private String softwareVersion;
    private String hardwarePn;
    private String hardwareVersion;
    /** ECU 软件模型：SINGLE_IMAGE/MULTI_TARGET（可空=未声明） */
    private String softwareModel;
    /** legacy SINGLE_IMAGE 槽位（只读兼容） */
    private String slot;
    /** legacy SINGLE_IMAGE 活动槽（只读兼容） */
    private Boolean active;
    /** 规范软件单元列表（两种模型统一后的权威业务来源） */
    private List<SoftwareUnitCmd> softwareUnits;

    /**
     * 转换为 Proto EcuVersion（canonicalizer 输入）。
     */
    public EcuVersion toEcuVersion() {
        EcuVersion.Builder b = EcuVersion.newBuilder()
                .setEcuId(ecuId)
                .setHardwarePartNumber(hardwarePn == null ? "" : hardwarePn)
                .setHwVersion(hardwareVersion == null ? "" : hardwareVersion)
                .setSoftwarePartNumber(softwarePn == null ? "" : softwarePn)
                .setSwVersion(softwareVersion == null ? "" : softwareVersion);
        if (softwareModel != null) {
            b.setSoftwareModel(EcuSoftwareModel.valueOf("ECU_SOFTWARE_MODEL_" + softwareModel));
        }
        if (slot != null) {
            b.setSlot(slot);
        }
        if (active != null) {
            b.setActive(active);
        }
        if (softwareUnits != null) {
            for (SoftwareUnitCmd u : softwareUnits) {
                SoftwareUnitVersion.Builder ub = SoftwareUnitVersion.newBuilder()
                        .setSoftwareTargetCode(u.getSoftwareTargetCode())
                        .setSoftwarePartNumber(u.getSoftwarePartNumber())
                        .setSwVersion(u.getSwVersion());
                if (u.getSlot() != null) {
                    ub.setSlot(u.getSlot());
                }
                if (u.getActive() != null) {
                    ub.setActive(u.getActive());
                }
                if (u.getDigest() != null) {
                    ub.setSoftwareDigest(vehicle.fota.v1.Types.Digest.newBuilder()
                            .setAlgorithm("sha256").setValueHex(u.getDigest()));
                }
                b.addSoftwareUnits(ub);
            }
        }
        return b.build();
    }
}
