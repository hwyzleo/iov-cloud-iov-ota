package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Canonical Digest 服务测试（CR-019 §4.2）
 *
 * @author hwyz_leo
 */
@DisplayName("CanonicalDigestService canonicalization-v2（CR-019）")
class CanonicalDigestServiceTest {

    private CanonicalVehicleInventory inventory(CanonicalEcu... ecus) {
        return CanonicalVehicleInventory.builder()
                .vin("LSVAU2188N2ZG4G")
                .inventoryRevision(3L)
                .collectedAt(Instant.parse("2026-09-08T00:00:00Z"))
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
    @DisplayName("ECU 排序无关性：同一内容摘要一致")
    void ecu_order_independent() {
        CanonicalSoftwareUnit u1 = new CanonicalSoftwareUnit("BOOT", "SPN-B", "V1.0", null, true, null);
        CanonicalSoftwareUnit u2 = new CanonicalSoftwareUnit("APP", "SPN-A", "V2.0", null, true, null);
        byte[] d1 = CanonicalDigestService.digestV2(
                inventory(ecu("ECU2", u2), ecu("ECU1", u1)));
        byte[] d2 = CanonicalDigestService.digestV2(
                inventory(ecu("ECU1", u1), ecu("ECU2", u2)));
        assertArrayEquals(d1, d2);
    }

    @Test
    @DisplayName("软件单元排序无关性：Target/Slot/PN/version 排序后摘要一致")
    void unit_order_independent() {
        CanonicalSoftwareUnit boot = new CanonicalSoftwareUnit("BOOT", "SPN-B", "V1.0", null, true, null);
        CanonicalSoftwareUnit app = new CanonicalSoftwareUnit("APP", "SPN-A", "V2.0", null, true, null);
        byte[] d1 = CanonicalDigestService.digestV2(inventory(ecu("ECU1", app, boot)));
        byte[] d2 = CanonicalDigestService.digestV2(inventory(ecu("ECU1", boot, app)));
        assertArrayEquals(d1, d2);
    }

    @Test
    @DisplayName("VIN/revision/collectedAt 不进入摘要：不同 VIN 同软件内容摘要一致")
    void vin_not_in_digest() {
        CanonicalSoftwareUnit u = new CanonicalSoftwareUnit("ECU_IMAGE", "SPN1", "V1.0", null, true, null);
        byte[] d1 = CanonicalDigestService.digestV2(
                inventory(ecu("ECU1", u)));
        byte[] d2 = CanonicalDigestService.digestV2(
                CanonicalVehicleInventory.builder()
                        .vin("ANOTHERVIN12345")
                        .inventoryRevision(9L)
                        .collectedAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .canonicalizationVersion(2)
                        .ecuList(List.of(ecu("ECU1", u)))
                        .build());
        assertArrayEquals(d1, d2);
    }

    @Test
    @DisplayName("软件变化 → 摘要变化")
    void software_change_changes_digest() {
        CanonicalSoftwareUnit u1 = new CanonicalSoftwareUnit("ECU_IMAGE", "SPN1", "V1.0", null, true, null);
        CanonicalSoftwareUnit u2 = new CanonicalSoftwareUnit("ECU_IMAGE", "SPN1", "V1.1", null, true, null);
        byte[] d1 = CanonicalDigestService.digestV2(inventory(ecu("ECU1", u1)));
        byte[] d2 = CanonicalDigestService.digestV2(inventory(ecu("ECU1", u2)));
        assertFalse(java.util.Arrays.equals(d1, d2));
    }

    @Test
    @DisplayName("摘要长度 32 字节")
    void digest_length() {
        CanonicalSoftwareUnit u = new CanonicalSoftwareUnit("ECU_IMAGE", "SPN1", "V1.0", null, true, null);
        assertEquals(32, CanonicalDigestService.digestV2(inventory(ecu("ECU1", u))).length);
    }
}
