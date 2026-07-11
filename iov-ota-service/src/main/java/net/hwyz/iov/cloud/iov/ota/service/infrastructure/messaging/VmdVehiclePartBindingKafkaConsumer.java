package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VMD车辆零件绑定变更事件Kafka消费者
 * <p>
 * 监听 vmd-vehicle-binding-changed，同步车辆零件只读投影。
 * 仅接受VMD来源的绑定写；BIND/REPLACE -> upsert，UNBIND -> 删除。
 * </p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.vmd.binding.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class VmdVehiclePartBindingKafkaConsumer {

    private final VehiclePartMapper vehiclePartMapper;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${ota.vmd.binding.kafka.topic:vmd-vehicle-binding-changed}",
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-iov-ota}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onVehiclePartBindingChanged(ConsumerRecord<String, String> record) {
        log.info("收到VMD车辆零件绑定变更事件: topic={}, key={}, offset={}",
                record.topic(), record.key(), record.offset());
        try {
            JsonNode root = objectMapper.readTree(record.value());
            String vin = root.path("vin").asText("");
            String partCode = root.path("partCode").asText("");
            String vehicleNodeCode = root.path("vehicleNodeCode").asText("");
            String changeType = root.path("changeType").asText("");

            log.info("处理VMD绑定变更: vin={}, partCode={}, vehicleNodeCode={}, changeType={}",
                    vin, partCode, vehicleNodeCode, changeType);

            if ("UNBIND".equals(changeType)) {
                Map<String, Object> params = new HashMap<>();
                params.put("vin", vin);
                params.put("pn", partCode);
                List<VehiclePartPo> existing = vehiclePartMapper.selectPoByMap(params);
                for (VehiclePartPo po : existing) {
                    vehiclePartMapper.physicalDeletePo(po.getId());
                }
                log.info("已删除车辆零件投影: vin={}, partCode={}", vin, partCode);
            } else {
                Map<String, Object> params = new HashMap<>();
                params.put("vin", vin);
                params.put("pn", partCode);
                List<VehiclePartPo> existingList = vehiclePartMapper.selectPoByMap(params);
                VehiclePartPo po = existingList.isEmpty() ? new VehiclePartPo() : existingList.get(0);
                po.setVin(vin);
                po.setPn(partCode);
                po.setDeviceCode(vehicleNodeCode);
                if (existingList.isEmpty()) {
                    vehiclePartMapper.insertPo(po);
                } else {
                    vehiclePartMapper.updatePo(po);
                }
                log.info("已同步车辆零件投影: vin={}, partCode={}", vin, partCode);
            }
        } catch (Exception e) {
            log.error("处理VMD绑定变更事件失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        }
    }
}
