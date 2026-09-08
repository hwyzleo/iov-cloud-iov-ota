package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vehicle.fota.v1.Types.Digest;
import vehicle.fota.v1.Types.EcuSoftwareModel;
import vehicle.fota.v1.Types.EcuVersion;
import vehicle.fota.v1.Types.SoftwareUnitVersion;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 清单规范化器测试（CR-019 §4.1/§3.2）
 *
 * @author hwyz_leo
 */
@DisplayName("InventoryCanonicalizer 清单规范化（CR-019）")
class InventoryCanonicalizerTest {

    private static final String VIN = "LSVAU2188N2ZG4G";

    @Test
    @DisplayName("存量 UNSPECIFIED + legacy → 规范化为 ECU_IMAGE 单单元")
    void legacy_unspecified_canonicalized_to_ecu_image() {
        EcuVersion ecu = EcuVersion.newBuilder()
                .setEcuId("ECU1")
                .setHardwarePartNumber("HPN1")
                .setHwVersion("H1")
                .setSoftwarePartNumber("SPN1")
                .setSwVersion("V1.0")
                .build();
        CanonicalVehicleInventory inv = InventoryCanonicalizer.canonicalize(
                VIN, 1L, Instant.now(), 1, List.of(ecu));
        CanonicalEcu cEcu = inv.getEcuList().get(0);
        assertEquals(1, cEcu.getSoftwareUnits().size());
        CanonicalSoftwareUnit u = cEcu.getSoftwareUnits().get(0);
        assertEquals("ECU_IMAGE", u.softwareTargetCode());
        assertEquals("SPN1", u.softwarePartNumber());
        assertEquals("V1.0", u.swVersion());
        assertTrue(u.active());
    }

    @Test
    @DisplayName("显式 SINGLE_IMAGE + legacy slot → ECU_IMAGE 带槽")
    void single_image_with_slot_canonicalized() {
        EcuVersion ecu = EcuVersion.newBuilder()
                .setEcuId("ECU1")
                .setSoftwarePartNumber("SPN1").setSwVersion("V1.0")
                .setSoftwareModel(EcuSoftwareModel.ECU_SOFTWARE_MODEL_SINGLE_IMAGE)
                .setSlot("A").setActive(true)
                .build();
        CanonicalVehicleInventory inv = InventoryCanonicalizer.canonicalize(
                VIN, 1L, Instant.now(), 2, List.of(ecu));
        CanonicalSoftwareUnit u = inv.getEcuList().get(0).getSoftwareUnits().get(0);
        assertEquals("A", u.slot());
        assertTrue(u.active());
    }

    @Test
    @DisplayName("MULTI_TARGET → 保留全部 Target/Slot 单元")
    void multi_target_preserves_units() {
        EcuVersion ecu = EcuVersion.newBuilder()
                .setEcuId("ECU1")
                .setSoftwareModel(EcuSoftwareModel.ECU_SOFTWARE_MODEL_MULTI_TARGET)
                .addSoftwareUnits(SoftwareUnitVersion.newBuilder()
                        .setSoftwareTargetCode("BOOT").setSoftwarePartNumber("SPN-B").setSwVersion("V1.0"))
                .addSoftwareUnits(SoftwareUnitVersion.newBuilder()
                        .setSoftwareTargetCode("APP").setSoftwarePartNumber("SPN-A").setSwVersion("V2.0")
                        .setSlot("A").setActive(true))
                .addSoftwareUnits(SoftwareUnitVersion.newBuilder()
                        .setSoftwareTargetCode("APP").setSoftwarePartNumber("SPN-A").setSwVersion("V2.0")
                        .setSlot("B").setActive(false))
                .build();
        CanonicalVehicleInventory inv = InventoryCanonicalizer.canonicalize(
                VIN, 1L, Instant.now(), 2, List.of(ecu));
        assertEquals(3, inv.getEcuList().get(0).getSoftwareUnits().size());
    }

    @Test
    @DisplayName("MULTI_TARGET 携带 legacy 软件字段 → 模式冲突拒绝")
    void multi_target_with_legacy_rejected() {
        EcuVersion ecu = EcuVersion.newBuilder()
                .setEcuId("ECU1")
                .setSoftwareModel(EcuSoftwareModel.ECU_SOFTWARE_MODEL_MULTI_TARGET)
                .setSoftwarePartNumber("SPN1").setSwVersion("V1.0")
                .addSoftwareUnits(SoftwareUnitVersion.newBuilder()
                        .setSoftwareTargetCode("APP").setSoftwarePartNumber("SPN-A").setSwVersion("V2.0"))
                .build();
        assertThrows(IllegalStateException.class, () ->
                InventoryCanonicalizer.canonicalize(VIN, 1L, Instant.now(), 2, List.of(ecu)));
    }

    @Test
    @DisplayName("UNSPECIFIED + software_units → 禁止猜测拒绝")
    void unspecified_with_units_rejected() {
        EcuVersion ecu = EcuVersion.newBuilder()
                .setEcuId("ECU1")
                .addSoftwareUnits(SoftwareUnitVersion.newBuilder()
                        .setSoftwareTargetCode("APP").setSoftwarePartNumber("SPN-A").setSwVersion("V2.0"))
                .build();
        assertThrows(IllegalStateException.class, () ->
                InventoryCanonicalizer.canonicalize(VIN, 1L, Instant.now(), 1, List.of(ecu)));
    }

    @Test
    @DisplayName("UNSPECIFIED 且 legacy 为空 → 单镜像缺软件事实拒绝")
    void unspecified_empty_legacy_rejected() {
        EcuVersion ecu = EcuVersion.newBuilder().setEcuId("ECU1").build();
        assertThrows(IllegalStateException.class, () ->
                InventoryCanonicalizer.canonicalize(VIN, 1L, Instant.now(), 1, List.of(ecu)));
    }
}
