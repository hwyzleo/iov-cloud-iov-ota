package net.hwyz.iov.cloud.iov.ota.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OTA Kafka Topic 声明环境参数（CR-019 §7 / 对齐 MDM-DSN-CR-034 模式）
 *
 * <p>由 {@code OtaKafkaTopicDefinitionProvider} 读取并构造 KafkaTopicDefinition，
 * 供 FW-KAFKA Topic Provisioning 统一完成存在性检查、幂等创建与状态传播。
 * 分区数、副本数等环境参数经 Nacos 配置绑定覆盖。
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "ota.kafka.topic-provisioning")
public class OtaKafkaTopicProvisioningProperties {

    /**
     * Topic 分区数
     */
    private int partitions = 3;

    /**
     * Topic 副本数（默认 1，适配单节点 broker；生产多副本环境经 Nacos 覆盖）
     */
    private short replicationFactor = 1;
}
