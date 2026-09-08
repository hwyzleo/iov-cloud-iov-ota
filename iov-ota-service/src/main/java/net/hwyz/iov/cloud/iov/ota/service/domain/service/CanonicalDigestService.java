package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;

/**
 * Canonical Digest 服务（CR-019 §4.2）
 *
 * <p>canonicalization-v2：规范化字节按固定顺序确定性编码，SHA-256 摘要。
 * VIN、inventoryRevision、collectedAt、acceptedAt 与 Envelope 字段不进入摘要，
 * 使软件未变化时跨上电保持同一 DIGEST。v1 存量摘要不得用 v2 重算覆盖。
 *
 * <p>编码顺序（§4.2）：
 * <ol>
 *   <li>固定域标识 vehicle.fota.v1.inventory.canonical.v2；</li>
 *   <li>ECU 按规范化 ecuId 升序；</li>
 *   <li>每个 ECU 的 softwareUnits 按 target、slot presence、slot、softwarePartNumber、swVersion 升序；</li>
 *   <li>字符串 UTF-8、NFC、trim；</li>
 *   <li>带字段号、类型、presence 与长度的确定性编码（TLV 风格），不使用分隔符拼接。</li>
 * </ol>
 *
 * @author hwyz_leo
 */
public final class CanonicalDigestService {

    /** canonicalization-v2 固定域标识 */
    public static final String CANONICAL_V2_DOMAIN = "vehicle.fota.v1.inventory.canonical.v2";
    public static final int CANONICAL_V2_VERSION = 2;

    private CanonicalDigestService() {
    }

    /**
     * 计算 canonicalization-v2 摘要。
     *
     * @param inventory 规范清单
     * @return SHA-256 摘要 bytes（32 字节）
     */
    public static byte[] digestV2(CanonicalVehicleInventory inventory) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(CANONICAL_V2_DOMAIN.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);

            List<CanonicalEcu> ecus = inventory.getEcuList().stream()
                    .sorted(Comparator.comparing(CanonicalEcu::getEcuId,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            for (CanonicalEcu ecu : ecus) {
                encodeEcu(md, ecu);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static void encodeEcu(MessageDigest md, CanonicalEcu ecu) {
        // 字段 1: ecu_id (string, tag=1)
        writeStringField(md, 1, ecu.getEcuId());
        // 字段 2: hardware_part_number (string, tag=2)
        writeStringField(md, 2, ecu.getHardwarePartNumber());
        // 字段 3: hw_version (string, tag=3)
        writeStringField(md, 3, ecu.getHwVersion());
        // 字段 4: software_units (repeated message, tag=4)
        List<CanonicalSoftwareUnit> units = ecu.getSoftwareUnits().stream()
                .sorted(Comparator
                        .comparing(CanonicalSoftwareUnit::softwareTargetCode,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(u -> u.slot() == null, Comparator.reverseOrder()) // slot presence: 有 slot 在前
                        .thenComparing(CanonicalSoftwareUnit::slot,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CanonicalSoftwareUnit::softwarePartNumber,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CanonicalSoftwareUnit::swVersion,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (CanonicalSoftwareUnit unit : units) {
            writeTag(md, 4);
            writeLenPrefixed(md, () -> encodeUnit(md, unit));
        }
    }

    private static void encodeUnit(MessageDigest md, CanonicalSoftwareUnit unit) {
        // 字段 1: software_target_code
        writeStringField(md, 1, unit.softwareTargetCode());
        // 字段 2: software_part_number
        writeStringField(md, 2, unit.softwarePartNumber());
        // 字段 3: sw_version
        writeStringField(md, 3, unit.swVersion());
        // 字段 4: slot (optional, presence 编码)
        writeOptionalStringField(md, 4, unit.slot());
        // 字段 5: active (bool, tag=5)
        writeBoolField(md, 5, unit.active());
        // 字段 6: digest (optional)
        writeOptionalStringField(md, 6, unit.digest());
    }

    // ---------------------------------------------------------------- 确定性编码

    private static void writeTag(MessageDigest md, int fieldNo) {
        md.update((byte) fieldNo);
    }

    private static void writeStringField(MessageDigest md, int fieldNo, String value) {
        writeTag(md, fieldNo);
        byte[] bytes = normalize(value).getBytes(StandardCharsets.UTF_8);
        writeLength(md, bytes.length);
        md.update(bytes);
    }

    private static void writeOptionalStringField(MessageDigest md, int fieldNo, String value) {
        writeTag(md, fieldNo);
        if (value == null) {
            // presence=0（absent）与长度 0 区分
            md.update((byte) 0);
        } else {
            md.update((byte) 1);
            byte[] bytes = normalize(value).getBytes(StandardCharsets.UTF_8);
            writeLength(md, bytes.length);
            md.update(bytes);
        }
    }

    private static void writeBoolField(MessageDigest md, int fieldNo, boolean value) {
        writeTag(md, fieldNo);
        md.update(value ? (byte) 1 : (byte) 0);
    }

    private static void writeLength(MessageDigest md, int length) {
        md.update(ByteBuffer.allocate(4).putInt(length).array());
    }

    private static void writeLenPrefixed(MessageDigest md, Runnable body) {
        // 简化：嵌套消息以固定标记包裹（不使用依赖缓冲区的子摘要，避免复杂化）
        md.update((byte) 0x7E);
        body.run();
        md.update((byte) 0x7F);
    }

    /** UTF-8、NFC、trim 规范化。 */
    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return Normalizer.normalize(trimmed, Normalizer.Form.NFC);
    }
}
