package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.service.MdmProjectionSyncAppService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM TypeApprovalBaseline事件Kafka消费者
 * <p>
 * 监听 mdm.eead.typeApprovalBaseline.event，同步TA基线投影。
 * 仅消费 status=RELEASED/FROZEN。
 * </p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.mdm.typeApprovalBaseline.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmTypeApprovalBaselineKafkaConsumer {

    private final MdmProjectionSyncAppService mdmProjectionSyncAppService;

    @KafkaListener(
            topics = "${ota.mdm.typeApprovalBaseline.kafka.topic:mdm.eead.typeApprovalBaseline.event}",
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-iov-ota}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTypeApprovalBaselineEvent(ConsumerRecord<String, String> record) {
        log.info("收到MDM TypeApprovalBaseline事件: topic={}, key={}, offset={}",
                record.topic(), record.key(), record.offset());
        try {
            mdmProjectionSyncAppService.handleTypeApprovalBaselineEvent(record.value());
        } catch (Exception e) {
            log.error("处理MDM TypeApprovalBaseline事件失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        }
    }
}
