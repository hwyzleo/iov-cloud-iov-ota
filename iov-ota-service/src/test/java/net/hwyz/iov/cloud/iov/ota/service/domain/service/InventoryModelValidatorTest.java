package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 清单模型校验器测试（CR-019 §3.2/§3.3）
 *
 * @author hwyz_leo
 */
@DisplayName("InventoryModelValidator 清单模型校验（CR-019）")
class InventoryModelValidatorTest {

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

    private CanonicalSoftwareUnit unit(String target, String pn, String ver, String slot, boolean active) {
        return new CanonicalSoftwareUnit(target, pn, ver, slot, active, null);
    }

    @Test
    @DisplayName("合法单镜像 ECU_IMAGE 通过")
    void valid_single_image_passes() {
        assertDoesNotThrow(() -> InventoryModelValidator.validate(
                inventory(ecu("ECU1", unit("ECU_IMAGE", "SPN1", "V1.0", null, true)))));
    }

    @Test
    @DisplayName("合法多 Target 双 Slot 且唯一 active 通过")
    void valid_multi_target_passes() {
        assertDoesNotThrow(() -> InventoryModelValidator.validate(
                inventory(ecu("ECU1",
                        unit("BOOT", "SPN-B", "V1.0", null, true),
                        unit("APP", "SPN-A", "V2.0", "A", true),
                        unit("APP", "SPN-A", "V2.0", "B", false)))));
    }

    @Test
    @DisplayName("重复 Target+Slot 拒绝")
    void duplicate_target_slot_rejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                InventoryModelValidator.validate(inventory(ecu("ECU1",
                        unit("APP", "SPN-A", "V2.0", "A", true),
                        unit("APP", "SPN-A", "V2.0", "A", false)))));
        assertTrue(ex.getMessage().contains("重复 Target+Slot"));
    }

    @Test
    @DisplayName("多 Slot 无 active 拒绝")
    void multi_slot_without_active_rejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                InventoryModelValidator.validate(inventory(ecu("ECU1",
                        unit("APP", "SPN-A", "V2.0", "A", false),
                        unit("APP", "SPN-A", "V2.0", "B", false)))));
        assertTrue(ex.getMessage().contains("恰有一个 active"));
    }

    @Test
    @DisplayName("双 active 拒绝")
    void double_active_rejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                InventoryModelValidator.validate(inventory(ecu("ECU1",
                        unit("APP", "SPN-A", "V2.0", "A", true),
                        unit("APP", "SPN-A", "V2.0", "B", true)))));
        assertTrue(ex.getMessage().contains("恰有一个 active"));
    }

    @Test
    @DisplayName("active=false 且 slot 缺失拒绝")
    void active_false_no_slot_rejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                InventoryModelValidator.validate(inventory(ecu("ECU1",
                        unit("APP", "SPN-A", "V2.0", null, false)))));
        assertTrue(ex.getMessage().contains("active=false 且 slot 缺失"));
    }

    @Test
    @DisplayName("空软件件号/版本拒绝")
    void empty_pn_rejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                InventoryModelValidator.validate(inventory(ecu("ECU1",
                        unit("ECU_IMAGE", "", "V1.0", null, true)))));
        assertTrue(ex.getMessage().contains("软件零件号为空"));
    }

    @Test
    @DisplayName("slot 规范化：trim+大写、空串转 null")
    void slot_normalization() {
        assertEquals("A", InventoryModelValidator.normalizeSlot(" a "));
        assertNull(InventoryModelValidator.normalizeSlot("  "));
        assertNull(InventoryModelValidator.normalizeSlot(null));
        assertThrows(IllegalStateException.class, () -> InventoryModelValidator.normalizeSlot("C"));
    }

    @Test
    @DisplayName("target 规范化：大写稳定 code")
    void target_normalization() {
        assertEquals("APP", InventoryModelValidator.normalizeTarget(" app "));
        assertNull(InventoryModelValidator.normalizeTarget("  "));
    }
}
