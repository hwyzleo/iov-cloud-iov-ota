package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OTA Kafka 启动治理参数（CR-020 §4.24.3 / §5）
 *
 * <ul>
 *   <li>{@code ota.kafka.startup.initialize-producer-topics}：是否由 IOV-OTA 幂等初始化
 *       三个显式 allowlist 生产 Topic；本地开发可显式关闭，但默认不依赖 Broker
 *       auto.create.topics.enable 作为正式方案。</li>
 *   <li>{@code ota.kafka.startup.validation-mode}：启动校验失败处理策略；
 *       FAIL_FAST=阻断启动，WARN=仅告警继续启动。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "ota.kafka.startup")
public class OtaKafkaStartupProperties {

    /**
     * 是否初始化生产 Topic（默认 true）
     */
    private boolean initializeProducerTopics = true;

    /**
     * 启动校验模式（默认 FAIL_FAST）
     */
    private StartupValidationMode validationMode = StartupValidationMode.FAIL_FAST;

    /**
     * 启动校验失败处理策略。
     */
    public enum StartupValidationMode {

        /**
         * 消费 Topic 缺失 / 配置漂移 / 旧名称残留时阻断启动
         */
        FAIL_FAST,

        /**
         * 记录错误告警，继续启动（本地/排障用）
         */
        WARN
    }
}
