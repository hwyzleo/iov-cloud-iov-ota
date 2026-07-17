package net.hwyz.iov.cloud.iov.ota.service.application.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleProjectionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 车辆投影同步应用服务
 * <p>
 * CR-011: 消费 MDM/VMD VehicleProduceEvent，幂等 upsert 本地投影。
 * 按 vin + source_version 幂等，仅当 event.version > local.sourceVersion 时更新。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleProjectionSyncService {

    private final VehicleProjectionRepository vehicleProjectionRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 处理车辆生产事件
     * <p>
     * 事件payload为VehicleProduceEventEnvelope JSON，含eventId/eventType/aggregateType/aggregateId/version/occurredAt/payload。
     * 按 vin + source_version 幂等更新。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleVehicleProduceEvent(String messageJson) {
        try {
            JsonNode root = objectMapper.readTree(messageJson);

            // 解析信封字段
            String eventId = root.path("eventId").asText("");
            String eventType = root.path("eventType").asText("");
            String aggregateId = root.path("aggregateId").asText("");
            long version = root.path("version").asLong(0);
            String occurredAt = root.path("occurredAt").asText("");

            // 解析payload
            JsonNode payload = root.path("payload");
            if (payload.isMissingNode() || payload.isNull()) {
                log.warn("车辆生产事件缺少payload: eventId={}", eventId);
                return;
            }

            String vin = payload.path("vin").asText(aggregateId);
            if (StrUtil.isBlank(vin)) {
                log.warn("车辆生产事件缺少vin: eventId={}", eventId);
                return;
            }

            log.info("处理车辆生产事件: eventId={}, eventType={}, vin={}, version={}", eventId, eventType, vin, version);

            // 查询现有投影
            VehicleProjectionPo existing = vehicleProjectionRepository.findByVinForUpdate(vin).orElse(null);

            if (existing != null && existing.getSourceVersion() != null && existing.getSourceVersion() >= version) {
                log.info("跳过旧版本车辆生产事件: vin={}, existingVersion={}, incomingVersion={}", vin, existing.getSourceVersion(), version);
                return;
            }

            // 构造投影数据
            VehicleProjectionPo po = existing != null ? existing : new VehicleProjectionPo();
            po.setVin(vin);
            po.setProductionTime(parseDateTime(payload.path("produceTime").asText("")));
            po.setPlantCode(payload.path("plantCode").asText(null));
            po.setBrandCode(payload.path("brandCode").asText(null));
            po.setPlatformCode(payload.path("platformCode").asText(null));
            po.setCarLineCode(payload.path("carLineCode").asText(null));
            po.setModelCode(payload.path("modelCode").asText(null));
            po.setVariantCode(payload.path("variantCode").asText(null));
            po.setConfigurationCode(payload.path("configurationCode").asText(null));
            po.setSourceEventId(eventId);
            po.setSourceVersion(version);
            po.setSourceEventTime(parseDateTime(occurredAt));
            po.setLastSyncTime(new Date());

            if (existing != null) {
                // 更新现有投影
                vehicleProjectionRepository.update(po);
                log.info("更新车辆投影成功: vin={}, sourceVersion={}", vin, version);
            } else {
                // 插入新投影
                po.setCreateTime(new Date());
                po.setModifyTime(new Date());
                try {
                    vehicleProjectionRepository.insert(po);
                    log.info("插入车辆投影成功: vin={}, sourceVersion={}", vin, version);
                } catch (DuplicateKeyException e) {
                    // 并发插入冲突，回退到更新
                    log.warn("并发插入车辆投影冲突，回退更新: vin={}", vin);
                    existing = vehicleProjectionRepository.findByVinForUpdate(vin).orElse(null);
                    if (existing != null && existing.getSourceVersion() < version) {
                        po.setId(existing.getId());
                        po.setCreateTime(existing.getCreateTime());
                        vehicleProjectionRepository.update(po);
                        log.info("并发更新车辆投影成功: vin={}, sourceVersion={}", vin, version);
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理车辆生产事件失败: {}", e.getMessage(), e);
            throw new RuntimeException("处理车辆生产事件失败", e);
        }
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateTimeStr 日期时间字符串
     * @return Date对象
     */
    private Date parseDateTime(String dateTimeStr) {
        if (StrUtil.isBlank(dateTimeStr)) {
            return new Date();
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.warn("解析日期时间失败: {}, 使用当前时间", dateTimeStr);
            return new Date();
        }
    }

    /**
     * 按VIN回源查询并同步车辆投影
     *
     * @param vin 车架号
     * @return 车辆投影
     */
    @Transactional(rollbackFor = Exception.class)
    public VehicleProjectionPo syncByVin(String vin) {
        // TODO: 调用VMD Feign API按VIN查询车辆信息
        // VmdVehicleService.getByVin(vin)
        // 然后构造投影并保存
        log.info("按VIN回源查询车辆投影: vin={}", vin);
        return null;
    }
}
