package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.service.MdmProjectionSyncAppService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM SoftwareBaseline事件Kafka消费者
 * <p>
 * 监听 mdm.material.softwareBaseline.event，同步配置软件应装基线投影。
 * </p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.mdm.baseline.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmSoftwareBaselineKafkaConsumer {

    private final MdmProjectionSyncAppService mdmProjectionSyncAppService;

    @KafkaListener(
            topics = "${ota.mdm.baseline.kafka.topic:mdm.material.softwareBaseline.event}",
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-iov-ota}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSoftwareBaselineEvent(ConsumerRecord<String, String> record) {
        log.info("收到MDM SoftwareBaseline事件: topic={}, key={}, offset={}",
                record.topic(), record.key(), record.offset());
        try {
            mdmProjectionSyncAppService.handleSoftwareBaselineEvent(record.value());
        } catch (Exception e) {
            log.error("处理MDM SoftwareBaseline事件失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        }
    }
}
