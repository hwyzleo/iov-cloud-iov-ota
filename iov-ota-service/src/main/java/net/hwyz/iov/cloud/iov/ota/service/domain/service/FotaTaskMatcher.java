package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;

import java.util.List;

/**
 * FOTA 任务匹配领域服务（CR-019 §6.3）
 *
 * <p>按 ECU + Target 匹配软件件号、版本与可选 Slot 策略：
 * <ul>
 *   <li>SINGLE_IMAGE：按 ecuId + target=ECU_IMAGE + softwarePartNumber + version；</li>
 *   <li>MULTI_TARGET：按安装计划声明的 ecuId + target 独立匹配；目标集合缺失、
 *       重复或 active 不唯一均阻断（校验在 InventoryModelValidator 完成）；</li>
 *   <li>软件包/安装计划关联 software_target_code 缺省规范化为 ECU_IMAGE；</li>
 *   <li>Slot 默认不作为软件包身份，仅安装计划显式声明 A/B 策略时参与核验。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
public final class FotaTaskMatcher {

    private FotaTaskMatcher() {
    }

    /**
     * 安装计划声明的升级目标（ECU + Target + 软件件号 + 可选版本/Slot）。
     */
    public record UpgradeRequirement(String ecuId, String softwareTargetCode,
                                     String softwarePartNumber, String version, String slot) {

        /** 存量 NULL target 规范化为 ECU_IMAGE */
        public String effectiveTarget() {
            return softwareTargetCode == null || softwareTargetCode.isBlank()
                    ? CanonicalSoftwareUnit.LEGACY_TARGET : softwareTargetCode;
        }
    }

    /**
     * 判断车辆 Canonical Inventory 是否满足任务的全部升级目标。
     *
     * @param inventory    规范清单
     * @param requirements 安装计划声明的升级目标
     * @return 是否匹配（全部目标均可命中）
     */
    public static boolean isEligible(CanonicalVehicleInventory inventory,
                                     List<UpgradeRequirement> requirements) {
        if (inventory == null || inventory.getEcuList() == null || inventory.getEcuList().isEmpty()) {
            return false;
        }
        // 未声明升级目标（空计划）：不额外过滤，保持兼容可见
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        for (UpgradeRequirement req : requirements) {
            if (!matches(inventory, req)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(CanonicalVehicleInventory inventory, UpgradeRequirement req) {
        String target = req.effectiveTarget();
        for (CanonicalEcu ecu : inventory.getEcuList()) {
            if (!req.ecuId().equals(ecu.getEcuId())) {
                continue;
            }
            for (CanonicalSoftwareUnit unit : ecu.getSoftwareUnits()) {
                if (!target.equals(unit.softwareTargetCode())) {
                    continue;
                }
                if (req.softwarePartNumber() != null
                        && !req.softwarePartNumber().equals(unit.softwarePartNumber())) {
                    continue;
                }
                if (req.version() != null && !req.version().equals(unit.swVersion())) {
                    continue;
                }
                if (req.slot() != null && !req.slot().equals(unit.slot())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }
}
