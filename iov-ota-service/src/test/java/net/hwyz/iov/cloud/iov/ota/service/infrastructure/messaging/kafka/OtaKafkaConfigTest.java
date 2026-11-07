package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.backoff.BackOffExecution;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * OTA Kafka 生产器序列化器契约测试。
 *
 * <p>FW-KAFKA 的 {@code bytesReactiveKafkaProducerTemplate} 只覆盖 key.serializer，
 * value.serializer 会继承 Spring Boot 默认 StringSerializer，导致 raw Envelope bytes
 * 发送失败。本服务自定义的 byte[] 生产器必须显式声明 ByteArraySerializer。
 *
 * @author hwyz_leo
 */
@DisplayName("OtaKafkaConfig byte[] 生产器序列化器")
class OtaKafkaConfigTest {

    @Test
    @DisplayName("value 使用 ByteArraySerializer，key 使用 StringSerializer")
    void producer_properties_use_byte_array_value_serializer() {
        Map<String, Object> props = OtaKafkaConfig.producerProperties(new KafkaProperties());

        assertEquals(StringSerializer.class, props.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertEquals(ByteArraySerializer.class, props.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    }

    @Test
    @DisplayName("入站错误处理器提交已恢复记录，避免毒消息阻塞分区")
    void inbound_error_handler_commits_recovered_records() {
        KafkaQuarantineStore store = mock(KafkaQuarantineStore.class);
        KafkaQuarantineRecoverer recoverer =
                new KafkaQuarantineRecoverer(store, new KafkaMessagingMetricsService());

        CommonErrorHandler handler = new OtaKafkaConfig().otaKafkaInboundErrorHandler(recoverer);

        assertTrue(handler instanceof DefaultErrorHandler);
        // MANUAL ack 模式下 commitRecovered=false 会导致恢复后重复投递同一条记录
        assertEquals(true, ReflectionTestUtils.getField(handler, "commitRecovered"));
    }

    @Test
    @DisplayName("有界指数退避：间隔翻倍到上限后按最大次数停止")
    void bounded_backoff_stops_after_max_attempts() {
        BackOffExecution execution = OtaKafkaConfig.boundedExponentialBackOff(100, 4).start();

        assertEquals(100, execution.nextBackOff());
        assertEquals(200, execution.nextBackOff());
        assertEquals(400, execution.nextBackOff());
        assertEquals(800, execution.nextBackOff());   // 上限 8×initial
        assertEquals(BackOffExecution.STOP, execution.nextBackOff());
        assertEquals(BackOffExecution.STOP, execution.nextBackOff());
    }
}
