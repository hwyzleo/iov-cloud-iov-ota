package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OTA→VMD 车辆软件清单观测事件（CR-019 §7.1）
 *
 * <p>云服务消息，不使用 Proto、不归 PAR-PROTO 管理、不进入 vehicle.fota.v1
 * 车云 payload。字段契约由 iov-ota-api 定义，生产者（IOV-OTA Outbox Relay）
 * 与消费者（edd-vmd）共同版本治理。
 *
 * <p>topic: ota.vehicle-software-inventory.observed / key: VIN /
 * dlq: ota.vehicle-software-inventory.observed.dlq
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSoftwareInventoryObservedEvent {

    /** 事件 ID（UUID） */
    private String eventId;
    /** 观测身份 = SHA-256(vin + inventoryRevision + canonicalDigest) */
    private String observationKey;
    /** 车架号 */
    private String vin;
    /** 清单版本号 */
    private Long inventoryRevision;
    /** 清单模型：SINGLE_IMAGE / MULTI_TARGET */
    private String inventoryModel;
    /** 车端清单采集时间（ISO-8601） */
    private String collectedAt;
    /** 服务端成功受理时间（ISO-8601） */
    private String acceptedAt;
    /** canonicalization 版本 */
    private Integer canonicalizationVersion;
    /** canonical digest（小写 hex） */
    private String canonicalDigest;
    /** 明细（按 SoftwareUnit 展开：每行一个 ECU+Target+Slot） */
    private List<Item> items;

    /** 明细项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String ecuId;
        private String hardwarePartNumber;
        private String hwVersion;
        private String softwareTargetCode;
        private String softwarePartNumber;
        private String swVersion;
        private String slot;
        private Boolean active;
        private String digest;
    }
}
