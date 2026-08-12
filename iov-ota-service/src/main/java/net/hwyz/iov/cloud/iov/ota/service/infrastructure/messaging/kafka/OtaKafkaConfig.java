package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
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
 * OTA Kafka 装配配置（CR-013）
 *
 * <p>提供 OTA 业务 topic 专用 {@link ConcurrentKafkaListenerContainerFactory}：
 * AckMode.MANUAL + 关闭自动提交，保证「Kafka offset 提交 ≠ 业务成功」，
 * 业务成功后才由消费者显式提交 offset。
 *
 * @author hwyz_leo
 */
@Configuration
@EnableConfigurationProperties(OtaKafkaProperties.class)
public class OtaKafkaConfig {

    /**
     * OTA 业务上行监听容器工厂（MANUAL ack）。
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> otaKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        ConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
