package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.service.MdmProjectionSyncAppService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM SWIN定义事件Kafka消费者
 * <p>
 * 监听 mdm.eead.swin.event，同步SWIN定义与受管系统清单投影。
 * </p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.mdm.swin.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmSwinKafkaConsumer {

    private final MdmProjectionSyncAppService mdmProjectionSyncAppService;

    @KafkaListener(
            topics = "${ota.mdm.swin.kafka.topic:mdm.eead.swin.event}",
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-iov-ota}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSwinDefinitionEvent(ConsumerRecord<String, String> record) {
        log.info("收到MDM SWIN事件: topic={}, key={}, offset={}",
                record.topic(), record.key(), record.offset());
        try {
            mdmProjectionSyncAppService.handleSwinDefinitionEvent(record.value());
        } catch (Exception e) {
            log.error("处理MDM SWIN事件失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        }
    }
}
