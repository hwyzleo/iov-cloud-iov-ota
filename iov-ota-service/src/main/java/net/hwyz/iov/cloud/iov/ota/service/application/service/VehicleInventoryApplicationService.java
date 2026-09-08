package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.InventoryDisposition;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.InventoryItemCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.SoftwareUnitCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.InventoryCanonicalizer;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.CanonicalDigestService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.InventoryModelValidator;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventorySoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventorySoftwareUnitPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * 车辆清单应用服务（CR-019 §6.1/§6.2）
 *
 * <p>统一 Canonical Inventory 受理：
 * <ul>
 *   <li>FULL：解析→校验模型矩阵→规范化→计算 canonicalDigest→校验请求摘要/revision→
 *       单事务（upsert header + 替换 ECU Item + 替换 SoftwareUnit + 观测 Outbox 一次 + accepted_time）；</li>
 *   <li>DIGEST：命中同 VIN/revision/canonicalizationVersion/digest 复用已接受 FULL，
 *       不更新 accepted_time、不发新事件；未命中/版本不同返回 FULL_REQUIRED。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleInventoryApplicationService {

    private final VehicleInventoryMapper vehicleInventoryMapper;
    private final VehicleInventoryItemMapper vehicleInventoryItemMapper;
    private final VehicleInventorySoftwareUnitMapper vehicleInventorySoftwareUnitMapper;
    private final InventoryObservedOutboxProducer observedOutboxProducer;

    /**
     * 处理清单握手（FULL/DIGEST）。
     *
     * @param cmd 检测命令
     * @return 清单处置结果
     */
    @Transactional
    public InventoryHandlingResult handleInventory(DetectionCmd cmd) {
        String mode = cmd.getInventoryMode();
        if ("FULL".equals(mode)) {
            return handleFull(cmd);
        }
        if ("DIGEST".equals(mode)) {
            return handleDigest(cmd);
        }
        return InventoryHandlingResult.builder()
                .disposition(InventoryDisposition.FULL_REQUIRED)
                .build();
    }

    // ---------------------------------------------------------------- FULL

    private InventoryHandlingResult handleFull(DetectionCmd cmd) {
        if (cmd.getInventoryItems() == null || cmd.getInventoryItems().isEmpty()) {
            return InventoryHandlingResult.builder()
                    .disposition(InventoryDisposition.FULL_REQUIRED)
                    .build();
        }
        long revision = cmd.getInventoryRevision() != null ? cmd.getInventoryRevision() : 1L;
        int canonicalizationVersion = resolveCanonicalizationVersion(cmd);
        Instant collectedAt = cmd.getCollectedAt() != null
                ? Instant.ofEpochMilli(cmd.getCollectedAt()) : null;

        // 幂等/冲突预检：相同 vin + revision + canonicalizationVersion
        VehicleInventoryPo existing = vehicleInventoryMapper.selectByVinAndRevision(cmd.getVin(), revision);
        if (existing != null) {
            if (existing.getCanonicalizationVersion() != null
                    && !Objects.equals(existing.getCanonicalizationVersion(), canonicalizationVersion)) {
                return InventoryHandlingResult.builder()
                        .disposition(InventoryDisposition.ALGORITHM_UNSUPPORTED)
                        .build();
            }
            // 同 revision 不同摘要 → 冲突（digest 以请求摘要为准，v2 用服务端计算）
            boolean sameDigest;
            if (existing.getCanonicalDigest() != null) {
                byte[] reqDigest = computeRequestDigest(cmd);
                sameDigest = reqDigest != null
                        && java.util.Arrays.equals(existing.getCanonicalDigest(), reqDigest);
            } else {
                // v1：legacy 字符串摘要比对
                sameDigest = cmd.getInventoryDigest() != null
                        && Objects.equals(existing.getDigest(), cmd.getInventoryDigest());
            }
            if (sameDigest) {
                // 幂等重放：复用原受理，不更新 accepted_time、不发新事件
                log.info("车辆[{}]FULL 清单幂等重放，复用原受理：revision[{}]", cmd.getVin(), revision);
                return InventoryHandlingResult.builder()
                        .disposition(InventoryDisposition.ACCEPTED)
                        .inventoryModel(deriveModel(cmd))
                        .canonicalizationVersion(canonicalizationVersion)
                        .build();
            }
            return InventoryHandlingResult.builder()
                    .disposition(InventoryDisposition.REVISION_CONFLICT)
                    .build();
        }

        // 规范化 + 校验 + 摘要
        CanonicalVehicleInventory canonical;
        try {
            canonical = InventoryCanonicalizer.canonicalize(
                    cmd.getVin(), revision, collectedAt, canonicalizationVersion,
                    toEcuVersion(cmd.getInventoryItems()));
            InventoryModelValidator.validate(canonical);
        } catch (IllegalStateException e) {
            log.warn("车辆[{}]FULL 清单校验失败：{}", cmd.getVin(), e.getMessage());
            return InventoryHandlingResult.builder()
                    .disposition(InventoryDisposition.FULL_REQUIRED)
                    .build();
        }

        byte[] canonicalDigest = canonicalizationVersion >= 2
                ? CanonicalDigestService.digestV2(canonical) : null;

        // 请求摘要校验（车端上报摘要若存在且与云端计算不一致 → DIGEST_MISMATCH）
        if (cmd.getInventoryDigest() != null && canonicalDigest != null) {
            String clientHex = cmd.getInventoryDigest().toLowerCase(java.util.Locale.ROOT);
            String serverHex = HexFormat.of().formatHex(canonicalDigest);
            if (!clientHex.equals(serverHex)) {
                return InventoryHandlingResult.builder()
                        .disposition(InventoryDisposition.DIGEST_MISMATCH)
                        .build();
            }
        }

        // 单事务持久化：header + items + software units + 观测 Outbox
        Instant acceptedAt = Instant.now();
        VehicleInventoryPo header = VehicleInventoryPo.builder()
                .vin(cmd.getVin())
                .inventoryRevision(revision)
                .digest(cmd.getInventoryDigest())
                .algorithm(cmd.getDigestAlgorithm() != null ? cmd.getDigestAlgorithm() : "SHA-256")
                .canonicalizationVersion(canonicalizationVersion)
                .canonicalDigest(canonicalDigest)
                .sourceCollectedAt(collectedAt == null ? null : Date.from(collectedAt))
                .acceptedTime(Date.from(acceptedAt))
                .build();
        vehicleInventoryMapper.insert(header);

        String model = deriveModel(cmd);
        for (CanonicalEcu ecu : canonical.getEcuList()) {
            VehicleInventoryItemPo itemPo = VehicleInventoryItemPo.builder()
                    .inventoryId(header.getId())
                    .ecuId(ecu.getEcuId())
                    .ecuName(null)
                    .softwarePn(legacySoftwarePn(ecu))
                    .softwareVersion(legacySwVersion(ecu))
                    .hardwarePn(ecu.getHardwarePartNumber())
                    .hardwareVersion(ecu.getHwVersion())
                    .softwareModel(model)
                    .legacySlot(legacySlot(ecu))
                    .legacyActive(legacyActive(ecu))
                    .build();
            vehicleInventoryItemMapper.insert(itemPo);
            for (CanonicalSoftwareUnit unit : ecu.getSoftwareUnits()) {
                VehicleInventorySoftwareUnitPo unitPo = VehicleInventorySoftwareUnitPo.builder()
                        .inventoryId(header.getId())
                        .inventoryItemId(itemPo.getId())
                        .vin(cmd.getVin())
                        .ecuId(ecu.getEcuId())
                        .softwareTargetCode(unit.softwareTargetCode())
                        .softwarePartNumber(unit.softwarePartNumber())
                        .swVersion(unit.swVersion())
                        .slot(unit.slot())
                        .active(unit.active())
                        .digest(unit.digest())
                        .build();
                vehicleInventorySoftwareUnitMapper.insert(unitPo);
            }
        }

        // 观测 Outbox（同事务）；v1 无 canonical digest 时以请求摘要参与观测键
        observedOutboxProducer.produce(canonical, canonicalDigest != null
                ? canonicalDigest : computeRequestDigest(cmd), model, acceptedAt);

        log.info("车辆[{}]FULL 清单已受理：revision[{}] model[{}] canonVersion[{}] ecuCount[{}]",
                cmd.getVin(), revision, model, canonicalizationVersion, canonical.getEcuList().size());
        return InventoryHandlingResult.builder()
                .disposition(InventoryDisposition.ACCEPTED)
                .inventoryModel(model)
                .canonicalizationVersion(canonicalizationVersion)
                .canonicalDigestHex(canonicalDigest == null ? null : HexFormat.of().formatHex(canonicalDigest))
                .build();
    }

    // ---------------------------------------------------------------- DIGEST

    private InventoryHandlingResult handleDigest(DetectionCmd cmd) {
        VehicleInventoryPo existing = vehicleInventoryMapper.selectLatestByVin(cmd.getVin());
        if (existing == null) {
            return InventoryHandlingResult.builder()
                    .disposition(InventoryDisposition.FULL_REQUIRED)
                    .build();
        }
        if (cmd.getInventoryRevision() != null
                && !cmd.getInventoryRevision().equals(existing.getInventoryRevision())) {
            return InventoryHandlingResult.builder()
                    .disposition(InventoryDisposition.REVISION_CONFLICT)
                    .build();
        }
        int requestedVersion = cmd.getCanonicalizationVersion() != null
                ? cmd.getCanonicalizationVersion() : 1;
        int storedVersion = existing.getCanonicalizationVersion() != null
                ? existing.getCanonicalizationVersion() : 1;
        if (requestedVersion != storedVersion) {
            return InventoryHandlingResult.builder()
                    .disposition(InventoryDisposition.FULL_REQUIRED)
                    .build();
        }
        if (storedVersion >= 2) {
            // v2：按 canonical digest 匹配
            byte[] reqDigest = cmd.getInventoryDigest() == null ? null
                    : java.util.HexFormat.of().parseHex(cmd.getInventoryDigest());
            byte[] stored = existing.getCanonicalDigest();
            if (stored == null || reqDigest == null || !java.util.Arrays.equals(stored, reqDigest)) {
                return InventoryHandlingResult.builder()
                        .disposition(InventoryDisposition.DIGEST_MISMATCH)
                        .build();
            }
        } else {
            // v1：legacy digest/algorithm 匹配
            if (cmd.getInventoryDigest() != null && !cmd.getInventoryDigest().equals(existing.getDigest())) {
                return InventoryHandlingResult.builder()
                        .disposition(InventoryDisposition.DIGEST_MISMATCH)
                        .build();
            }
            if (cmd.getDigestAlgorithm() != null && !cmd.getDigestAlgorithm().equals(existing.getAlgorithm())) {
                return InventoryHandlingResult.builder()
                        .disposition(InventoryDisposition.ALGORITHM_UNSUPPORTED)
                        .build();
            }
        }
        // DIGEST 命中：复用已接受 FULL，不更新 accepted_time、不发新事件
        log.info("车辆[{}]DIGEST 清单命中，复用已受理 FULL：revision[{}]", cmd.getVin(),
                existing.getInventoryRevision());
        return InventoryHandlingResult.builder()
                .disposition(InventoryDisposition.ACCEPTED)
                .inventoryModel(deriveStoredModel(existing))
                .canonicalizationVersion(storedVersion)
                .build();
    }

    /**
     * 从最新清单的 ECU Item 推导 inventory model（header 不存 model）。
     */
    private String deriveStoredModel(VehicleInventoryPo header) {
        List<VehicleInventoryItemPo> items = vehicleInventoryItemMapper.selectByInventoryId(header.getId());
        boolean multi = items.stream()
                .anyMatch(i -> "MULTI_TARGET".equals(i.getSoftwareModel()));
        return multi ? "MULTI_TARGET" : "SINGLE_IMAGE";
    }

    // ---------------------------------------------------------------- helpers

    private int resolveCanonicalizationVersion(DetectionCmd cmd) {
        int version = cmd.getCanonicalizationVersion() != null
                ? cmd.getCanonicalizationVersion() : 1;
        // 任一 ECU 使用 MULTI_TARGET/software_units 时版本必须为 2（Proto 侧已强制，这里兜底）
        return version;
    }

    private byte[] computeRequestDigest(DetectionCmd cmd) {
        if (cmd.getInventoryDigest() == null) {
            return null;
        }
        try {
            return java.util.HexFormat.of().parseHex(cmd.getInventoryDigest());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String deriveModel(DetectionCmd cmd) {
        boolean multi = cmd.getInventoryItems().stream()
                .anyMatch(i -> "MULTI_TARGET".equals(i.getSoftwareModel()));
        return multi ? "MULTI_TARGET" : "SINGLE_IMAGE";
    }

    private static List<vehicle.fota.v1.Types.EcuVersion> toEcuVersion(List<InventoryItemCmd> items) {
        return items.stream().map(InventoryItemCmd::toEcuVersion).toList();
    }

    private static String legacySoftwarePn(CanonicalEcu ecu) {
        return ecu.getSoftwareUnits().isEmpty() ? null
                : ecu.getSoftwareUnits().get(0).softwarePartNumber();
    }

    private static String legacySwVersion(CanonicalEcu ecu) {
        return ecu.getSoftwareUnits().isEmpty() ? null
                : ecu.getSoftwareUnits().get(0).swVersion();
    }

    private static String legacySlot(CanonicalEcu ecu) {
        return ecu.getSoftwareUnits().isEmpty() ? null
                : ecu.getSoftwareUnits().get(0).slot();
    }

    private static Boolean legacyActive(CanonicalEcu ecu) {
        return ecu.getSoftwareUnits().isEmpty() ? null
                : ecu.getSoftwareUnits().get(0).active();
    }

    /**
     * 清单处置结果。
     */
    @Getter
    @Builder
    public static class InventoryHandlingResult {
        private final InventoryDisposition disposition;
        private final String inventoryModel;
        private final Integer canonicalizationVersion;
        private final String canonicalDigestHex;
    }
}
