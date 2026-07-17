package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleProjectionSyncService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * VMD车辆生产事件Kafka消费者
 * <p>
 * 监听 vmd.vehicle.produce.event，同步车辆主档本地只读投影。
 * 以 VIN + 上游版本幂等 upsert。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.vmd.vehicle-produce.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class VehicleProduceEventConsumer {

    private final VehicleProjectionSyncService vehicleProjectionSyncService;

    @KafkaListener(
            topics = "${ota.vmd.vehicle-produce.kafka.topic:vmd.vehicle.produce.event}",
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-iov-ota}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onVehicleProduceEvent(ConsumerRecord<String, String> record) {
        log.info("收到VMD车辆生产事件: topic={}, key={}, offset={}",
                record.topic(), record.key(), record.offset());
        try {
            vehicleProjectionSyncService.handleVehicleProduceEvent(record.value());
        } catch (Exception e) {
            log.error("处理VMD车辆生产事件失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        }
    }
}
