package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.config.OtaKafkaTopicProvisioningProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OTA Kafka Topic 定义提供者测试（CR-019 §7 / 对齐 MDM-DSN-CR-034）
 *
 * @author hwyz_leo
 */
@DisplayName("OtaKafkaTopicDefinitionProvider Topic 声明（CR-019）")
class OtaKafkaTopicDefinitionProviderTest {

    @Test
    @DisplayName("声明 OTA 作为 Producer 的全部 Topic（含 DLQ），不声明上行消费 Topic")
    void declares_all_producer_topics() {
        OtaKafkaTopicProvisioningProperties props = new OtaKafkaTopicProvisioningProperties();
        props.setPartitions(3);
        props.setReplicationFactor((short) 1);
        OtaKafkaTopicDefinitionProvider provider = new OtaKafkaTopicDefinitionProvider(props);

        Collection<KafkaTopicDefinition> defs = provider.topicDefinitions();
        var names = defs.stream().map(KafkaTopicDefinition::name).collect(Collectors.toSet());

        assertTrue(names.contains("iov.vagw.down.fota"));
        assertTrue(names.contains("iov.vagw.up.fota.dlq"));
        assertTrue(names.contains("ota.vehicle-software-inventory.observed"));
        assertTrue(names.contains("ota.vehicle-software-inventory.observed.dlq"));
        // 上行消费 Topic 不声明（由车云接入契约/上游治理）
        assertFalse(names.contains("iov.vagw.up.fota"));
        assertFalse(names.contains("iov.vagw.delivery.fota"));

        for (KafkaTopicDefinition def : defs) {
            assertEquals(3, def.partitions());
            assertEquals((short) 1, def.replicationFactor());
        }
    }
}
