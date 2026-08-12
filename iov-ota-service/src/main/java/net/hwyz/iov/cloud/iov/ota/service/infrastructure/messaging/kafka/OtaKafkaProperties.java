package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OTA Kafka 消息化配置（CR-013）
 *
 * <p>物理 Topic、分区数、保留期由车云接入契约治理；IOV-OTA 维护 Envelope 与 payload 业务语义。
 * 由 {@code @EnableConfigurationProperties(OtaKafkaProperties.class)} 注册。
 *
 * @author hwyz_leo
 */
@Data
@ConfigurationProperties(prefix = "ota.kafka")
public class OtaKafkaProperties {

    /** 上行消费 */
    private Inbound inbound = new Inbound();

    /** 下行生产 */
    private Outbound outbound = new Outbound();

    /** 死信 */
    private Dlq dlq = new Dlq();

    @Data
    public static class Inbound {
        /** 是否启用上行消费 */
        private boolean enabled = true;
        /** 上行业务事件 topic（逗号分隔） */
        private String topics = "ota.cloud.event";
        /** 消费组 */
        private String groupId = "iov-cloud-iov-ota";
        /** 并发消费者数 */
        private int concurrency = 3;
    }

    @Data
    public static class Outbound {
        /** 是否启用下行生产 */
        private boolean enabled = true;
        /** 下行结果/命令/回执 topic */
        private String topic = "ota.cloud.command";
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
    public static class Dlq {
        /** 死信/隔离 topic */
        private String topic = "ota.cloud.dlq";
    }
}
