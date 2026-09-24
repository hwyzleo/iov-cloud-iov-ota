package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OTA 生产 Topic 幂等初始化器（CR-020 §4.24.2 / §4.24.3 / RD-020-2）
 *
 * <p>向 FW-KAFKA 声明仅由 IOV-OTA 生产的三个显式 allowlist Topic：
 * ota.fota / ota.fota.dlq.up / ota.vehicle-software-inventory.observed。
 * 由框架统一完成 Catalog 合并、存在性检查、幂等创建（TopicAlreadyExists 视为成功）、
 * 后台重试与状态传播；不接受通配符或后缀推导，不创建消费 Topic 或目录外 Topic。
 *
 * <p>Topic 名称来自 {@link OtaKafkaTopicsProperties}（目录基线）；
 * 分区数、副本数、retention 等环境参数来自 {@link OtaKafkaTopicDefinitionProperties}。
 * 本地开发可显式关闭（{@code ota.kafka.startup.initialize-producer-topics=false}）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.kafka.startup.initialize-producer-topics",
        havingValue = "true", matchIfMissing = true)
public class OtaProducerTopicInitializer implements KafkaTopicDefinitionProvider {

    private final OtaKafkaTopicsProperties topics;
    private final OtaKafkaTopicDefinitionProperties definitions;

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        // 显式 allowlist：三个生产 Topic，不包含消费 Topic，不使用通配符/后缀推导
        Set<String> allowlist = new LinkedHashSet<>(List.of(
                topics.getFotaDown(),
                topics.getFotaUpDlq(),
                topics.getInventoryObserved()));

        List<KafkaTopicDefinition> result = new ArrayList<>(allowlist.size());
        for (String name : allowlist) {
            OtaKafkaTopicDefinitionProperties.Definition definition = definitions.getTopics().get(name);
            int partitions = definition != null && definition.getPartitions() != null
                    ? definition.getPartitions() : definitions.getDefaultPartitions();
            short replicas = definition != null && definition.getReplicationFactor() != null
                    ? definition.getReplicationFactor() : definitions.getDefaultReplicationFactor();
            Map<String, String> configs = definition != null && definition.getConfigs() != null
                    ? definition.getConfigs() : Map.of();
            result.add(new KafkaTopicDefinition(name, partitions, replicas, configs));
        }
        log.info("OTA 生产 Topic 定义（显式 allowlist）：{}", allowlist);
        return result;
    }
}
