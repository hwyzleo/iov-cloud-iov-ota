package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * OTA Kafka 装配配置（CR-014 / CR-019）
 *
 * <p>提供 FOTA 业务 topic 专用 {@link ConcurrentKafkaListenerContainerFactory}：
 * Key=String、Value=byte[]（完整序列化 VehicleMessageEnvelope / GatewayDeliveryStatus bytes）、
 * AckMode.MANUAL + 关闭自动提交，保证「Kafka offset 提交 ≠ 业务成功」，
 * 业务成功后才由消费者显式提交 offset。Kafka Header 仅用于观测。
 *
 * <p>CR-019：显式提供 {@link Admin}（FW-KAFKA Topic Provisioning 使用，对齐
 * MDM-DSN-CR-034 模式，避免与 Spring Boot KafkaAutoConfiguration 的 kafkaAdmin
 * Bean 名冲突）。运行 Principal 需具备 DescribeTopics / CreateTopics 权限。
 *
 * <p>另行装配 value=byte[] 的 {@link ReactiveKafkaProducerTemplate}：FW-KAFKA 的
 * {@code bytesReactiveKafkaProducerTemplate} 历史上只覆盖 key.serializer，value.serializer
 * 继承 Spring Boot 默认 StringSerializer，发送 raw Envelope bytes 时抛
 * {@code ClassCastException: [B cannot be cast to String}（已在该 starter 修正）。
 * 本服务仍显式装配并标记 {@link Primary}，不依赖依赖版本行为，并保证
 * Envelope/DLQ/Outbox 下行生产的 value 与 {@code byte[]} 契约一致。
 *
 * <p>入站错误处理（DefaultErrorHandler）：可恢复失败按指数退避有界重试，耗尽后由
 * {@link KafkaQuarantineRecoverer} 落盘隔离并提交 offset，避免一条毒消息永久阻塞分区；
 * 必须 setCommitRecovered(true)，否则 MANUAL ack 模式下恢复器执行后 offset 仍不会推进。
 *
 * @author hwyz_leo
 */
@Configuration
@EnableConfigurationProperties(OtaKafkaProperties.class)
public class OtaKafkaConfig {

    /** 可恢复失败的最大重试次数（耗尽后隔离落盘） */
    @Value("${ota.kafka.inbound.error-handler.max-retry:3}")
    private long inboundMaxRetry = 3;

    /** 指数退避初始间隔（毫秒） */
    @Value("${ota.kafka.inbound.error-handler.backoff-ms:1000}")
    private long inboundBackoffMs = 1000;

    /**
     * Kafka Admin 客户端（FW-KAFKA Topic Provisioning 使用），应用关闭时释放资源。
     */
    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(KafkaProperties properties) {
        return AdminClient.create(properties.buildAdminProperties());
    }

    /**
     * Envelope 下行/隔离生产器（Key=String, Value=raw bytes）。
     *
     * <p>显式指定 {@link ByteArraySerializer}，避免继承 Spring Boot 默认 StringSerializer
     * 导致 {@code byte[]} value 序列化失败。
     */
    @Bean
    @Primary
    public ReactiveKafkaProducerTemplate<String, byte[]> byteArrayReactiveKafkaProducerTemplate(
            KafkaProperties kafkaProperties) {
        return new ReactiveKafkaProducerTemplate<>(
                SenderOptions.create(producerProperties(kafkaProperties)));
    }

    /**
     * 构造 byte[] value 生产器的 Producer 配置（可单测）。
     */
    static Map<String, Object> producerProperties(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return props;
    }

    /**
     * 入站错误处理器：指数退避有界重试 + 耗尽后落盘隔离（可回放），不再无限重投毒消息。
     */
    @Bean
    public CommonErrorHandler otaKafkaInboundErrorHandler(KafkaQuarantineRecoverer recoverer) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer, boundedExponentialBackOff(inboundBackoffMs, inboundMaxRetry));
        // MANUAL ack 模式下容器只在 commitRecovered=true 时提交已恢复记录的 offset，
        // 否则恢复后仍会重复投递同一条记录（分区阻塞）。
        handler.setCommitRecovered(true);
        return handler;
    }

    /**
     * 有界指数退避：间隔从 initialMs 开始翻倍（上限 8×initialMs），最多尝试 maxAttempts 次。
     *
     * <p>补偿 spring-core 6.0.x 的 ExponentialBackOff 没有最大尝试次数上限：只用
     * maxElapsedTime 收敛会让大间隔配置提前耗尽，这里显式以次数收敛。
     */
    static BackOff boundedExponentialBackOff(long initialMs, long maxAttempts) {
        long initial = Math.max(1, initialMs);
        long cap = Math.max(initial, initial * 8);
        long limit = Math.max(1, maxAttempts);
        return () -> new BackOffExecution() {
            private long next = initial;
            private long attempts = 0;

            @Override
            public long nextBackOff() {
                if (attempts >= limit) {
                    return BackOffExecution.STOP;
                }
                attempts++;
                long current = next;
                next = Math.min(cap, next * 2);
                return current;
            }
        };
    }

    /**
     * FOTA 上行/投递监听容器工厂（Key=String, Value=byte[], MANUAL ack）。
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> fotaKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties, CommonErrorHandler otaKafkaInboundErrorHandler) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        ConsumerFactory<String, byte[]> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(otaKafkaInboundErrorHandler);
        return factory;
    }
}
