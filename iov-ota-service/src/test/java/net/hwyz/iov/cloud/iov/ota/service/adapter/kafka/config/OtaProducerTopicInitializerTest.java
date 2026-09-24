package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OTA 生产 Topic 初始化器测试（CR-020 §7 初始化）
 *
 * <p>只声明三个显式 allowlist 生产 Topic（ota.fota / ota.fota.dlq.up /
 * ota.vehicle-software-inventory.observed），不包含消费 Topic、旧名称或目录外 Topic；
 * 分区数、副本数与 configs 按 Topic 独立注入。
 *
 * @author hwyz_leo
 */
@DisplayName("OtaProducerTopicInitializer 显式 allowlist（CR-020）")
class OtaProducerTopicInitializerTest {

    @Test
    @DisplayName("只声明三个生产 Topic，不含消费 Topic 与旧名称")
    void declares_only_explicit_producer_allowlist() {
        OtaKafkaTopicsProperties topics = new OtaKafkaTopicsProperties();
        OtaKafkaTopicDefinitionProperties definitions = new OtaKafkaTopicDefinitionProperties();
        OtaProducerTopicInitializer initializer = new OtaProducerTopicInitializer(topics, definitions);

        Collection<KafkaTopicDefinition> defs = initializer.topicDefinitions();
        var names = defs.stream().map(KafkaTopicDefinition::name).collect(Collectors.toSet());

        assertEquals(3, names.size());
        assertTrue(names.contains("ota.fota"));
        assertTrue(names.contains("ota.fota.dlq.up"));
        assertTrue(names.contains("ota.vehicle-software-inventory.observed"));

        // 消费 Topic 不声明（由 VAGW/上游治理，缺失时启动期 fail-fast）
        assertFalse(names.contains("vagw.fota"));
        assertFalse(names.contains("vagw.fota.dlq.up"));
        // 旧名称与目录外 Topic 不声明
        assertFalse(names.contains("iov.vagw.up.fota"));
        assertFalse(names.contains("iov.vagw.down.fota"));
        assertFalse(names.contains("iov.vagw.delivery.fota"));
        assertFalse(names.contains("ota.vehicle-software-inventory.observed.dlq"));
    }

    @Test
    @DisplayName("未配置 Topic 定义时使用默认分区/副本")
    void uses_defaults_when_no_per_topic_definition() {
        OtaKafkaTopicsProperties topics = new OtaKafkaTopicsProperties();
        OtaKafkaTopicDefinitionProperties definitions = new OtaKafkaTopicDefinitionProperties();
        definitions.setDefaultPartitions(5);
        definitions.setDefaultReplicationFactor((short) 2);
        OtaProducerTopicInitializer initializer = new OtaProducerTopicInitializer(topics, definitions);

        Collection<KafkaTopicDefinition> defs = initializer.topicDefinitions();
        for (KafkaTopicDefinition def : defs) {
            assertEquals(5, def.partitions());
            assertEquals((short) 2, def.replicationFactor());
            assertTrue(def.configs().isEmpty());
        }
    }

    @Test
    @DisplayName("按 Topic 独立配置分区/副本/retention，并保留顺序")
    void per_topic_definition_applied_in_allowlist_order() {
        OtaKafkaTopicsProperties topics = new OtaKafkaTopicsProperties();
        OtaKafkaTopicDefinitionProperties definitions = new OtaKafkaTopicDefinitionProperties();

        OtaKafkaTopicDefinitionProperties.Definition fota = new OtaKafkaTopicDefinitionProperties.Definition();
        fota.setPartitions(6);
        fota.setReplicationFactor((short) 3);
        fota.setConfigs(Map.of("retention.ms", "604800000"));
        definitions.getTopics().put("ota.fota", fota);

        OtaKafkaTopicDefinitionProperties.Definition dlq = new OtaKafkaTopicDefinitionProperties.Definition();
        dlq.setPartitions(1);
        definitions.getTopics().put("ota.fota.dlq.up", dlq);

        OtaProducerTopicInitializer initializer = new OtaProducerTopicInitializer(topics, definitions);
        Collection<KafkaTopicDefinition> defs = initializer.topicDefinitions();

        assertEquals(List.of("ota.fota", "ota.fota.dlq.up",
                "ota.vehicle-software-inventory.observed"),
                defs.stream().map(KafkaTopicDefinition::name).toList());

        Map<String, KafkaTopicDefinition> byName = defs.stream()
                .collect(Collectors.toMap(KafkaTopicDefinition::name, d -> d));
        assertEquals(6, byName.get("ota.fota").partitions());
        assertEquals((short) 3, byName.get("ota.fota").replicationFactor());
        assertEquals("604800000", byName.get("ota.fota").configs().get("retention.ms"));
        assertEquals(1, byName.get("ota.fota.dlq.up").partitions());
        assertEquals(definitions.getDefaultReplicationFactor(),
                byName.get("ota.fota.dlq.up").replicationFactor());
        assertEquals(definitions.getDefaultPartitions(),
                byName.get("ota.vehicle-software-inventory.observed").partitions());
    }
}
