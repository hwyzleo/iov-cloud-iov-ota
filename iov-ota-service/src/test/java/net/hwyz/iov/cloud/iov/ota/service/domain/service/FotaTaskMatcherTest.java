package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FOTA 任务匹配器测试（CR-019 §6.3）
 *
 * @author hwyz_leo
 */
@DisplayName("FotaTaskMatcher 任务匹配（CR-019）")
class FotaTaskMatcherTest {

    private CanonicalVehicleInventory inventory(CanonicalEcu... ecus) {
        return CanonicalVehicleInventory.builder()
                .vin("LSVAU2188N2ZG4G")
                .inventoryRevision(1L)
                .canonicalizationVersion(2)
                .ecuList(List.of(ecus))
                .build();
    }

    private CanonicalEcu ecu(String ecuId, CanonicalSoftwareUnit... units) {
        return CanonicalEcu.builder()
                .ecuId(ecuId)
                .hardwarePartNumber("HPN1")
                .hwVersion("H1")
                .softwareUnits(List.of(units))
                .build();
    }

    @Test
    @DisplayName("SINGLE_IMAGE：按 ecuId + ECU_IMAGE + PN + version 命中")
    void single_image_match() {
        CanonicalVehicleInventory inv = inventory(ecu("ECU1",
                new CanonicalSoftwareUnit("ECU_IMAGE", "SPN1", "V1.0", null, true, null)));
        assertTrue(FotaTaskMatcher.isEligible(inv, List.of(
                new FotaTaskMatcher.UpgradeRequirement("ECU1", null, "SPN1", "V1.0", null))));
        // 版本不匹配 → 不命中
        assertFalse(FotaTaskMatcher.isEligible(inv, List.of(
                new FotaTaskMatcher.UpgradeRequirement("ECU1", null, "SPN1", "V2.0", null))));
    }

    @Test
    @DisplayName("MULTI_TARGET：按安装计划声明的 ecuId + target 独立匹配")
    void multi_target_match() {
        CanonicalVehicleInventory inv = inventory(ecu("ECU1",
                new CanonicalSoftwareUnit("BOOT", "SPN-B", "V1.0", null, true, null),
                new CanonicalSoftwareUnit("APP", "SPN-A", "V2.0", null, true, null)));
        assertTrue(FotaTaskMatcher.isEligible(inv, List.of(
                new FotaTaskMatcher.UpgradeRequirement("ECU1", "BOOT", "SPN-B", null, null),
                new FotaTaskMatcher.UpgradeRequirement("ECU1", "APP", "SPN-A", null, null))));
        // 目标缺失 → 阻断
        assertFalse(FotaTaskMatcher.isEligible(inv, List.of(
                new FotaTaskMatcher.UpgradeRequirement("ECU1", "CALIBRATION", "SPN-C", null, null))));
    }

    @Test
    @DisplayName("Slot 策略：显式声明 A/B 时参与核验")
    void slot_policy_match() {
        CanonicalVehicleInventory inv = inventory(ecu("ECU1",
                new CanonicalSoftwareUnit("APP", "SPN-A", "V2.0", "A", true, null),
                new CanonicalSoftwareUnit("APP", "SPN-A", "V2.0", "B", false, null)));
        assertTrue(FotaTaskMatcher.isEligible(inv, List.of(
                new FotaTaskMatcher.UpgradeRequirement("ECU1", "APP", "SPN-A", null, "A"))));
        assertFalse(FotaTaskMatcher.isEligible(inv, List.of(
                new FotaTaskMatcher.UpgradeRequirement("ECU1", "APP", "SPN-A", null, "C"))));
    }

    @Test
    @DisplayName("空计划不额外过滤（兼容可见）")
    void empty_requirements_pass() {
        CanonicalVehicleInventory inv = inventory(ecu("ECU1",
                new CanonicalSoftwareUnit("ECU_IMAGE", "SPN1", "V1.0", null, true, null)));
        assertTrue(FotaTaskMatcher.isEligible(inv, List.of()));
    }
}
