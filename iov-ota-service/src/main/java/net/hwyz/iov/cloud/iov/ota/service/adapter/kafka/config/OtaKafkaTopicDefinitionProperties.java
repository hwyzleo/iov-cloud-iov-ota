package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OTA 生产 Kafka Topic 环境参数（CR-020 §4.24.2）
 *
 * <p>保存由 IOV-OTA 初始化（Producer 角色）的三个 Topic 的 partitions、replicas
 * 与 retention 等关键配置，供 {@link OtaProducerTopicInitializer} 构造
 * KafkaTopicDefinition；环境参数经 Nacos 配置绑定覆盖，生产默认值不在代码中固化。
 *
 * <p>配置示例（{@code ota.kafka.topic-definitions.*}）：
 * <pre>
 * ota.kafka.topic-definitions.topics.ota.fota.partitions: ${OTA_FOTA_PARTITIONS:3}
 * ota.kafka.topic-definitions.topics.ota.fota.replication-factor: ${OTA_FOTA_REPLICAS:1}
 * ota.kafka.topic-definitions.topics.ota.fota.configs.retention-ms: ${OTA_FOTA_RETENTION_MS:604800000}
 * </pre>
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "ota.kafka.topic-definitions")
public class OtaKafkaTopicDefinitionProperties {

    /**
     * 默认分区数（Topic 未单独配置时使用）
     */
    private int defaultPartitions = 3;

    /**
     * 默认副本数（Topic 未单独配置时使用；默认 1 适配单节点 broker，生产多副本经 Nacos 覆盖）
     */
    private short defaultReplicationFactor = 1;

    /**
     * 生产 Topic 定义：key=Topic 名（allowlist 内）
     */
    private Map<String, Definition> topics = new LinkedHashMap<>();

    /**
     * 单个生产 Topic 定义。
     */
    @Data
    public static class Definition {

        /**
         * 分区数（为空时使用 {@link #defaultPartitions}）
         */
        private Integer partitions;

        /**
         * 副本数（为空时使用 {@link #defaultReplicationFactor}）
         */
        private Short replicationFactor;

        /**
         * 关键配置（retention.ms、cleanup.policy 等），仅用于创建与漂移校验，不自动修改已存在 Topic
         */
        private Map<String, String> configs = new LinkedHashMap<>();
    }
}
