package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * OTA Kafka 装配配置（CR-014）
 *
 * <p>提供 FOTA 业务 topic 专用 {@link ConcurrentKafkaListenerContainerFactory}：
 * Key=String、Value=byte[]（完整序列化 VehicleMessageEnvelope / GatewayDeliveryStatus bytes）、
 * AckMode.MANUAL + 关闭自动提交，保证「Kafka offset 提交 ≠ 业务成功」，
 * 业务成功后才由消费者显式提交 offset。Kafka Header 仅用于观测。
 *
 * @author hwyz_leo
 */
@Configuration
@EnableConfigurationProperties(OtaKafkaProperties.class)
public class OtaKafkaConfig {

    /**
     * FOTA 上行/投递监听容器工厂（Key=String, Value=byte[], MANUAL ack）。
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> fotaKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties) {
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
        return factory;
    }
}
