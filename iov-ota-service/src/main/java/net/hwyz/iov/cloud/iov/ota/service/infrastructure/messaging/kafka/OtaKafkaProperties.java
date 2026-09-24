package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OTA Kafka 消息化运行时参数（CR-013 / CR-020 §4.24.1）
 *
 * <p>Topic 名称（逻辑用途 → 实际 Topic 映射）统一由
 * {@link net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config.OtaKafkaTopicsProperties}
 * 管理（以 Kafka Topic 目录为基线），本类只保留消费/发布运行时参数，
 * 不再持有任何 Topic 字符串或旧名称默认值。
 * 由 {@code @EnableConfigurationProperties(OtaKafkaProperties.class)} 注册。
 *
 * @author hwyz_leo
 */
@Data
@ConfigurationProperties(prefix = "ota.kafka")
public class OtaKafkaProperties {

    /** 上行消费运行时参数 */
    private Inbound inbound = new Inbound();

    /** 下行生产运行时参数 */
    private Outbound outbound = new Outbound();

    /** OTA→VMD 云服务事件运行时参数（CR-019 §7） */
    private CloudEvents cloudEvents = new CloudEvents();

    @Data
    public static class Inbound {
        /** 是否启用上行消费 */
        private boolean enabled = true;
        /** 消费组 */
        private String groupId = "iov-cloud-iov-ota";
        /** 并发消费者数 */
        private int concurrency = 3;
    }

    @Data
    public static class Outbound {
        /** 是否启用下行生产 */
        private boolean enabled = true;
        /** 最大重试次数（超过转死信） */
        private int maxRetry = 5;
        /** 退避基础秒数（指数退避） */
        private long backoffBaseSeconds = 1;
        /** 每批拉取条数 */
        private int batchSize = 100;
        /** 轮询间隔（毫秒） */
        private long pollIntervalMs = 2000;
    }

    @Data
    public static class CloudEvents {
        /** 是否启用云服务事件发布 */
        private boolean enabled = true;
        /** 最大重试次数（超过转死信） */
        private int maxRetry = 5;
        /** 退避基础秒数（指数退避） */
        private long backoffBaseSeconds = 1;
        /** 每批拉取条数 */
        private int batchSize = 100;
        /** 轮询间隔（毫秒） */
        private long pollIntervalMs = 2000;
    }
}
