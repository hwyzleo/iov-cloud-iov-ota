package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleSoftwareInventoryObservedEvent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * 车辆清单观测消息生产者（CR-019 §6.4/§7）
 *
 * <p>从 Canonical Inventory 生成 OTA→VMD 云服务观测事件，并在 FULL 成功事务内
 * 以业务唯一键（VEHICLE_INVENTORY_OBSERVED + observation_key）追加到
 * tb_cloud_event_outbox。相同 FULL 重放复用原业务结果，不新增 Outbox。
 *
 * <p>observation_key = SHA-256(vin + inventoryRevision + canonicalDigest)
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryObservedOutboxProducer {

    private final CloudEventOutboxRepository cloudEventOutboxRepository;
    private final KafkaMessagingMetricsService metrics;

    /**
     * 计算观测业务唯一键。
     */
    public static String observationKey(String vin, long inventoryRevision, byte[] canonicalDigest) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(vin.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(Long.toString(inventoryRevision).getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            if (canonicalDigest != null) {
                md.update(canonicalDigest);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 生成并追加观测事件（须在 FULL 业务事务内调用，同事务提交）。
     *
     * @param inventory            规范清单
     * @param canonicalDigest      canonical digest bytes（v2 时非空）
     * @param inventoryModel       SINGLE_IMAGE / MULTI_TARGET
     * @param acceptedAt           服务端受理时间
     * @return true=新建 Outbox；false=相同业务键已存在（幂等重放）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean produce(CanonicalVehicleInventory inventory, byte[] canonicalDigest,
                           String inventoryModel, Instant acceptedAt) {
        String digestHex = canonicalDigest == null ? "" : HexFormat.of().formatHex(canonicalDigest);
        String key = observationKey(inventory.getVin(), inventory.getInventoryRevision(), canonicalDigest);
        String businessKey = CloudEventOutboxPo.EVENT_INVENTORY_OBSERVED + ":" + key;

        if (cloudEventOutboxRepository.findByBusinessKey(businessKey) != null) {
            metrics.increment("ota.inventory.observed.replay");
            log.debug("车辆[{}]清单观测幂等命中，不新建 Outbox：observationKey[{}]",
                    inventory.getVin(), key);
            return false;
        }

        VehicleSoftwareInventoryObservedEvent event = build(inventory, key, inventoryModel,
                digestHex, acceptedAt);
        String payload = toJson(event);

        CloudEventOutboxPo po = CloudEventOutboxPo.builder()
                .eventType(CloudEventOutboxPo.EVENT_INVENTORY_OBSERVED)
                .businessKey(businessKey)
                .payloadJson(payload)
                .vin(inventory.getVin())
                .build();
        boolean created = cloudEventOutboxRepository.append(po);
        if (created) {
            metrics.increment("ota.inventory.observed.created");
            log.info("车辆[{}]清单观测事件已入 Outbox：revision[{}] observationKey[{}]",
                    inventory.getVin(), inventory.getInventoryRevision(), key);
        }
        return created;
    }

    private static VehicleSoftwareInventoryObservedEvent build(
            CanonicalVehicleInventory inventory, String observationKey,
            String inventoryModel, String canonicalDigestHex, Instant acceptedAt) {
        List<VehicleSoftwareInventoryObservedEvent.Item> items = new ArrayList<>();
        for (CanonicalEcu ecu : inventory.getEcuList()) {
            for (CanonicalSoftwareUnit unit : ecu.getSoftwareUnits()) {
                items.add(VehicleSoftwareInventoryObservedEvent.Item.builder()
                        .ecuId(ecu.getEcuId())
                        .hardwarePartNumber(ecu.getHardwarePartNumber())
                        .hwVersion(ecu.getHwVersion())
                        .softwareTargetCode(unit.softwareTargetCode())
                        .softwarePartNumber(unit.softwarePartNumber())
                        .swVersion(unit.swVersion())
                        .slot(unit.slot())
                        .active(unit.active())
                        .digest(unit.digest())
                        .build());
            }
        }
        return VehicleSoftwareInventoryObservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .observationKey(observationKey)
                .vin(inventory.getVin())
                .inventoryRevision(inventory.getInventoryRevision())
                .inventoryModel(inventoryModel)
                .collectedAt(inventory.getCollectedAt() == null ? null : inventory.getCollectedAt().toString())
                .acceptedAt(acceptedAt.toString())
                .canonicalizationVersion(inventory.getCanonicalizationVersion())
                .canonicalDigest(canonicalDigestHex.isEmpty() ? null : canonicalDigestHex)
                .items(items)
                .build();
    }

    private static String toJson(VehicleSoftwareInventoryObservedEvent event) {
        try {
            return com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .findAndAddModules()
                    .build()
                    .writeValueAsString(event);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("观测事件 JSON 序列化失败", e);
        }
    }
}
