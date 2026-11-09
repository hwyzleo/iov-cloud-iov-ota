package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * VMD车辆零件绑定变更事件Kafka消费者
 * <p>
 * 监听 vmd.vehicle-part-binding.changed（配置键 ota.kafka.topics.vmd-part-binding-changed，目录治理拼写），同步车辆零件只读投影。
 * 仅接受VMD来源的绑定写；BIND/REPLACE -> upsert，UNBIND -> 删除。
 * </p>
 * <p>
 * 查询必须使用 vin + pn 业务身份键（{@code selectPoListByVinAndPn}）：
 * 早期版本使用无过滤条件的 {@code selectPoByMap}，导致所有事件命中同一行
 * （全表 id 最大行）永不插入，且会把其他 VIN 的行改写/删除。
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
            topics = "${ota.kafka.topics.vmd-part-binding-changed:vmd.vehicle-part-binding.changed}",
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
                // 必须按 vin + pn 业务身份键定位命中行：通用 selectPoByMap 无过滤条件时会返回全表，
                // 会导致一条 UNBIND 事件物理删除所有车辆的零件投影。
                List<VehiclePartPo> existing = vehiclePartMapper.selectPoListByVinAndPn(vin, partCode);
                for (VehiclePartPo po : existing) {
                    vehiclePartMapper.physicalDeletePo(po.getId());
                }
                log.info("已删除车辆零件投影: vin={}, partCode={}, rows={}", vin, partCode, existing.size());
            } else {
                List<VehiclePartPo> existingList = vehiclePartMapper.selectPoListByVinAndPn(vin, partCode);
                if (existingList.isEmpty()) {
                    VehiclePartPo po = new VehiclePartPo();
                    po.setVin(vin);
                    po.setPn(partCode);
                    po.setDeviceCode(vehicleNodeCode);
                    try {
                        vehiclePartMapper.insertPo(po);
                        log.info("已新增车辆零件投影: vin={}, partCode={}, deviceCode={}, id={}",
                                vin, partCode, vehicleNodeCode, po.getId());
                    } catch (DuplicateKeyException e) {
                        // CR-023 C1：并发「先查后插」被 uk_vehicle_part_vin_pn 拦截，转为更新避免事件丢失
                        List<VehiclePartPo> concurrent = vehiclePartMapper.selectPoListByVinAndPn(vin, partCode);
                        if (concurrent.isEmpty()) {
                            throw e;
                        }
                        VehiclePartPo concurrentPo = concurrent.get(0);
                        concurrentPo.setDeviceCode(vehicleNodeCode);
                        vehiclePartMapper.updatePo(concurrentPo);
                        log.info("车辆零件投影并发插入命中唯一键，已转为更新: vin={}, partCode={}, id={}",
                                vin, partCode, concurrentPo.getId());
                    }
                } else {
                    VehiclePartPo po = existingList.get(0);
                    po.setDeviceCode(vehicleNodeCode);
                    vehiclePartMapper.updatePo(po);
                    log.info("已更新车辆零件投影: vin={}, partCode={}, deviceCode={}, id={}",
                            vin, partCode, vehicleNodeCode, po.getId());
                }
            }
        } catch (Exception e) {
            log.error("处理VMD绑定变更事件失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        }
    }
}
