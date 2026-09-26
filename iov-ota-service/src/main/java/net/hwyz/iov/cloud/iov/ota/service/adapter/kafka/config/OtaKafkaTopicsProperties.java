package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OTA Kafka Topic 统一配置（CR-020 §4.24.1 / RD-020-1）
 *
 * <p>逻辑用途 → 实际 Topic 的唯一映射，以 Kafka Topic 目录为基线：
 * <ul>
 *   <li>{@code ota.kafka.topics.fota-up} → vagw.fota（Consumer）；</li>
 *   <li>{@code ota.kafka.topics.vagw-up-dlq} → vagw.fota.dlq.up（Consumer，VAGW 生产）；</li>
 *   <li>{@code ota.kafka.topics.fota-down} → ota.fota（Producer）；</li>
 *   <li>{@code ota.kafka.topics.fota-up-dlq} → ota.fota.dlq.up（Producer）；</li>
 *   <li>{@code ota.kafka.topics.inventory-observed} → ota.vehicle-software-inventory.observed（Producer）。</li>
 *   <li>{@code ota.kafka.topics.vmd-vehicle-produce} → vmd.vehicle-produce（Consumer，VMD 生产）；</li>
 *   <li>{@code ota.kafka.topics.vmd-part-binding-changed} → vmd.vehcile-part-binding.changed（Consumer，VMD 生产）。</li>
 * </ul>
 *
 * <p>所有 Kafka Adapter、Inbox/Outbox、监控与测试只从该配置读取 Topic 名称，
 * 禁止在 Adapter 中散落硬编码默认字符串、通配符或后缀推导。
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "ota.kafka.topics")
public class OtaKafkaTopicsProperties {

    /** 上行主消息 Topic（FotaEnvelopeConsumer 消费，VAGW 生产） */
    private String fotaUp = "vagw.fota";

    /** VAGW 上行错误流 Topic（IOV-OTA 消费，VAGW 生产） */
    private String vagwUpDlq = "vagw.fota.dlq.up";

    /** 下行主消息 Topic（FotaEnvelopeProducer 生产，VAGW 消费） */
    private String fotaDown = "ota.fota";

    /** IOV-OTA 上行不可恢复错误 DLQ Topic（OtaKafkaDlqService 生产） */
    private String fotaUpDlq = "ota.fota.dlq.up";

    /** OTA→VMD 车辆软件清单观测 Topic（CloudEventOutboxRelay 生产） */
    private String inventoryObserved = "ota.vehicle-software-inventory.observed";

    /** VMD 车辆生产事件 Topic（VehicleProduceEventConsumer 消费，VMD 生产，目录治理值） */
    private String vmdVehicleProduce = "vmd.vehicle-produce";

    /** VMD 车辆-零件绑定变更 Topic（VmdVehiclePartBindingKafkaConsumer 消费，VMD 生产，保留 vehcile 目录拼写） */
    private String vmdPartBindingChanged = "vmd.vehcile-part-binding.changed";
}
