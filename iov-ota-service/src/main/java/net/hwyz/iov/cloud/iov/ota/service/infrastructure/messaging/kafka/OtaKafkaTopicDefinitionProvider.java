package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.config.OtaKafkaTopicProvisioningProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * OTA 下游 Kafka Topic 定义提供者（CR-019 §7 / 对齐 MDM-DSN-CR-034 模式）
 *
 * <p>向 FW-KAFKA 声明全部由 OTA 作为 Producer 发送的 Topic，由框架统一完成
 * Catalog 合并、存在性检查、幂等创建、后台重试与状态传播。
 *
 * <p>范围（OTA 负责预创建/检查的发送 Topic）：
 * <ul>
 *   <li>{@code iov.vagw.down.fota}：FOTA 下行 Envelope（FotaEnvelopeProducer）；</li>
 *   <li>{@code iov.vagw.up.fota.dlq}：上行死信/隔离（OtaKafkaDlqService 发送）；</li>
 *   <li>{@code ota.vehicle-software-inventory.observed}：OTA→VMD 云服务观测事件（CloudEventOutboxRelay）；</li>
 *   <li>{@code ota.vehicle-software-inventory.observed.dlq}：观测事件 DLQ。</li>
 * </ul>
 *
 * <p>OTA 不声明上行消费 Topic（iov.vagw.up.fota / iov.vagw.delivery.fota）——
 * 物理 topic 由车云接入契约/上游治理；也不声明清单外 Topic。
 * 分区数、副本数通过 {@link OtaKafkaTopicProvisioningProperties} 环境参数注入。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class OtaKafkaTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    private final OtaKafkaTopicProvisioningProperties properties;

    /** FOTA 下行 Envelope Topic（VAGW 消费） */
    private static final String FOTA_DOWN_TOPIC = "iov.vagw.down.fota";
    /** 上行死信/隔离 Topic（OtaKafkaDlqService 发送） */
    private static final String FOTA_UP_DLQ_TOPIC = "iov.vagw.up.fota.dlq";
    /** OTA→VMD 观测事件 Topic（CR-019） */
    private static final String INVENTORY_OBSERVED_TOPIC = "ota.vehicle-software-inventory.observed";
    /** 观测事件 DLQ */
    private static final String INVENTORY_OBSERVED_DLQ_TOPIC = "ota.vehicle-software-inventory.observed.dlq";

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        List<String> names = List.of(
                FOTA_DOWN_TOPIC,
                FOTA_UP_DLQ_TOPIC,
                INVENTORY_OBSERVED_TOPIC,
                INVENTORY_OBSERVED_DLQ_TOPIC);
        List<KafkaTopicDefinition> definitions = new ArrayList<>(names.size());
        for (String name : names) {
            definitions.add(new KafkaTopicDefinition(
                    name, properties.getPartitions(), properties.getReplicationFactor()));
        }
        return definitions;
    }
}
