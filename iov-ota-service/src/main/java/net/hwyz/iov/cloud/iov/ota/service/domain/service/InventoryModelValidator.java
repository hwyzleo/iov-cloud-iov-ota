package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 规范清单校验领域服务（CR-019 §3.2/§3.3）
 *
 * <p>无数据库副作用。校验通过后 Canonical Inventory 才可进入摘要、持久化与业务链。
 * 任一 ECU 软件单元非法则整份 FULL 拒绝（调用方回滚事务）。
 *
 * @author hwyz_leo
 */
public final class InventoryModelValidator {

    /** 当前批准槽位字典：仅 A/B，其他值需配置字典后启用 */
    private static final Set<String> APPROVED_SLOTS = Set.of("A", "B");

    private InventoryModelValidator() {
    }

    /**
     * 校验一份规范清单。
     *
     * @param inventory 规范清单
     * @throws IllegalStateException 校验失败（reason 含具体非法项）
     */
    public static void validate(CanonicalVehicleInventory inventory) {
        if (inventory == null || inventory.getEcuList() == null || inventory.getEcuList().isEmpty()) {
            throw new IllegalStateException("清单为空：无 ECU 明细");
        }
        Set<String> ecuIds = new HashSet<>();
        for (CanonicalEcu ecu : inventory.getEcuList()) {
            if (ecu.getEcuId() == null || ecu.getEcuId().isBlank()) {
                throw new IllegalStateException("ECU 标识为空");
            }
            if (!ecuIds.add(ecu.getEcuId())) {
                throw new IllegalStateException("重复 ECU: " + ecu.getEcuId());
            }
            validateEcu(ecu);
        }
    }

    private static void validateEcu(CanonicalEcu ecu) {
        if (ecu.getSoftwareUnits() == null || ecu.getSoftwareUnits().isEmpty()) {
            throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] 无软件单元");
        }
        // 同一 ecu_id + target：多个 Slot 时必须恰有一个 active=true；
        // 只有一个无 Slot 项时 active 缺省为 true（canonicalizer 已填充）。
        java.util.Map<String, List<CanonicalSoftwareUnit>> byTarget = new java.util.HashMap<>();
        Set<String> keys = new HashSet<>();
        for (CanonicalSoftwareUnit unit : ecu.getSoftwareUnits()) {
            if (unit.softwareTargetCode() == null || unit.softwareTargetCode().isBlank()) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] 软件目标编码为空");
            }
            if (unit.softwarePartNumber() == null || unit.softwarePartNumber().isBlank()) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] Target["
                        + unit.softwareTargetCode() + "] 软件零件号为空");
            }
            if (unit.swVersion() == null || unit.swVersion().isBlank()) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] Target["
                        + unit.softwareTargetCode() + "] 软件版本为空");
            }
            String slot = normalizeSlot(unit.slot());
            String key = unit.softwareTargetCode() + "|" + (slot == null ? "" : slot);
            if (!keys.add(key)) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] 重复 Target+Slot: " + key);
            }
            if (!unit.active() && slot == null) {
                throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] active=false 且 slot 缺失");
            }
            byTarget.computeIfAbsent(unit.softwareTargetCode(), k -> new ArrayList<>()).add(unit);
        }
        // 多 Slot 必须恰有一个 active
        for (var entry : byTarget.entrySet()) {
            List<CanonicalSoftwareUnit> units = entry.getValue();
            boolean multiSlot = units.size() > 1;
            if (multiSlot) {
                long activeCount = units.stream().filter(CanonicalSoftwareUnit::active).count();
                if (activeCount != 1) {
                    throw new IllegalStateException("ECU[" + ecu.getEcuId() + "] Target["
                            + entry.getKey() + "] 多 Slot 必须恰有一个 active，实际[" + activeCount + "]");
                }
            }
        }
    }

    /**
     * 槽位规范化：trim 后转大写；空串转 NULL；非法值拒绝。
     *
     * @return 规范化槽位或 null
     */
    public static String normalizeSlot(String slot) {
        if (slot == null) {
            return null;
        }
        String s = slot.trim();
        if (s.isEmpty()) {
            return null;
        }
        String upper = s.toUpperCase(java.util.Locale.ROOT);
        if (!APPROVED_SLOTS.contains(upper)) {
            throw new IllegalStateException("非法槽位: " + slot + "（当前批准值仅 A/B）");
        }
        return upper;
    }

    /**
     * 目标编码规范化：trim 后转大写稳定 code。
     */
    public static String normalizeTarget(String target) {
        if (target == null) {
            return null;
        }
        String t = target.trim();
        return t.isEmpty() ? null : t.toUpperCase(java.util.Locale.ROOT);
    }
}
