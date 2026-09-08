package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.InventoryModelValidator;
import vehicle.fota.v1.Types.EcuSoftwareModel;
import vehicle.fota.v1.Types.EcuVersion;
import vehicle.fota.v1.Types.SoftwareUnitVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 清单规范化器（CR-019 §4.1/§3.2）
 *
 * <p>将 legacy 单值或 software_units[] 统一转换为 Canonical Inventory：
 * <ul>
 *   <li>UNSPECIFIED + legacy → 兼容接受为 SINGLE_IMAGE，规范化为 ECU_IMAGE 单元；</li>
 *   <li>SINGLE_IMAGE + legacy(+slot/active) → ECU_IMAGE 单元；</li>
 *   <li>MULTI_TARGET + software_units → 保留全部 Target/Slot 单元。</li>
 * </ul>
 * 无数据库副作用，支持 golden payload 单元测试。
 *
 * @author hwyz_leo
 */
public final class InventoryCanonicalizer {

    private InventoryCanonicalizer() {
    }

    /**
     * 规范化一份 FULL 清单。
     *
     * @param vin                    车架号
     * @param inventoryRevision      清单版本号
     * @param collectedAt            车端采集时间
     * @param canonicalizationVersion canonicalization 版本（1/2）
     * @param ecuList                原始 EcuVersion 列表
     * @return 规范清单
     */
    public static CanonicalVehicleInventory canonicalize(
            String vin, long inventoryRevision, Instant collectedAt,
            int canonicalizationVersion, List<EcuVersion> ecuList) {
        if (ecuList == null || ecuList.isEmpty()) {
            throw new IllegalStateException("FULL 清单缺少 ecu_list");
        }
        List<CanonicalEcu> ecus = new ArrayList<>(ecuList.size());
        for (EcuVersion ecu : ecuList) {
            ecus.add(canonicalizeEcu(ecu));
        }
        CanonicalVehicleInventory inventory = CanonicalVehicleInventory.builder()
                .vin(vin)
                .inventoryRevision(inventoryRevision)
                .collectedAt(collectedAt)
                .canonicalizationVersion(canonicalizationVersion)
                .ecuList(ecus)
                .build();
        // 规范化后统一校验
        InventoryModelValidator.validate(inventory);
        return inventory;
    }

    private static CanonicalEcu canonicalizeEcu(EcuVersion ecu) {
        EcuSoftwareModel model = ecu.getSoftwareModel();
        List<CanonicalSoftwareUnit> units = new ArrayList<>();

        if (model == EcuSoftwareModel.ECU_SOFTWARE_MODEL_MULTI_TARGET) {
            // MULTI_TARGET：legacy 软件字段必须为空，全部事实来自 software_units
            if (hasLegacySoftware(ecu)) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId()
                        + "] MULTI_TARGET 携带 legacy 软件字段，模式冲突");
            }
            if (ecu.getSoftwareUnitsCount() == 0) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] MULTI_TARGET 缺少 software_units");
            }
            for (SoftwareUnitVersion u : ecu.getSoftwareUnitsList()) {
                units.add(new CanonicalSoftwareUnit(
                        InventoryModelValidator.normalizeTarget(u.getSoftwareTargetCode()),
                        u.getSoftwarePartNumber(),
                        u.getSwVersion(),
                        InventoryModelValidator.normalizeSlot(u.hasSlot() ? u.getSlot() : null),
                        u.hasActive() ? u.getActive() : true,
                        u.hasSoftwareDigest() ? u.getSoftwareDigest().getValueHex() : null));
            }
        } else {
            // UNSPECIFIED / SINGLE_IMAGE：legacy 单镜像面
            if (ecu.getSoftwareUnitsCount() > 0) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId()
                        + "] 未声明 MULTI_TARGET 却携带 software_units，禁止猜测");
            }
            if (!hasLegacySoftware(ecu)) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId()
                        + "] 单镜像缺少 software_part_number / sw_version");
            }
            boolean active = !ecu.hasActive() || ecu.getActive();
            String slot = InventoryModelValidator.normalizeSlot(ecu.hasSlot() ? ecu.getSlot() : null);
            if (!active && slot == null) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] active=false 且 slot 缺失");
            }
            units.add(new CanonicalSoftwareUnit(
                    CanonicalSoftwareUnit.LEGACY_TARGET,
                    ecu.getSoftwarePartNumber(),
                    ecu.getSwVersion(),
                    slot,
                    active,
                    null));
        }
        return CanonicalEcu.builder()
                .ecuId(ecu.getEcuId())
                .hardwarePartNumber(ecu.getHardwarePartNumber())
                .hwVersion(ecu.getHwVersion())
                .softwareUnits(units)
                .build();
    }

    private static boolean hasLegacySoftware(EcuVersion ecu) {
        return (ecu.getSoftwarePartNumber() != null && !ecu.getSoftwarePartNumber().isBlank())
                || (ecu.getSwVersion() != null && !ecu.getSwVersion().isBlank());
    }
}
