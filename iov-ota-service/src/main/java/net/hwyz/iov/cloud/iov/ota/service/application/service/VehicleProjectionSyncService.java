package net.hwyz.iov.cloud.iov.ota.service.application.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.RetryableProjectionException;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleProjectionNotFoundException;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleProjectionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.VmdVehicleProjectionGateway;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto.VmdVehicleProjectionDto;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto.VmdVehicleProjectionSnapshotDto;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.DataSyncRecordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.DataSyncRecordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 车辆投影同步应用服务（CR-011 / CR-015 §4）
 * <p>
 * 消费 MDM/VMD VehicleProduceEvent，幂等 upsert 本地投影；并完成
 * 按 VIN 回源（syncByVin）、游标快照 Bootstrap 与周期性对账。
 * 统一按 vin + source_version 幂等，仅当 version > local.sourceVersion 时更新。
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
    private final VmdVehicleProjectionGateway vmdVehicleProjectionGateway;
    private final DataSyncRecordMapper dataSyncRecordMapper;

    /** checkpoint 记录编码（tb_data_sync_record） */
    private static final int CHECKPOINT_SOURCE = 9;   // VMD
    private static final int CHECKPOINT_TYPE = 9;     // VEHICLE_PROJECTION
    private static final String CHECKPOINT_CODE = "vehicle-projection-sync";

    @Value("${ota.vehicle-projection.bootstrap.page-size:500}")
    private int bootstrapPageSize;

    @Value("${ota.vehicle-projection.bootstrap.max-pages:1000}")
    private int bootstrapMaxPages;

    /**
     * VMD 无时区墙上时间串的显式解释时区（CR-021 §4.25.3：不用 systemDefault 猜测）。
     * 配置键 ota.time.business-zone-id，默认 Asia/Shanghai（业务确认）。
     */
    @Value("${ota.time.business-zone-id:Asia/Shanghai}")
    private ZoneId businessZoneId;

    /** VMD 事件遗留无时区格式（CR-021 §4.25.1：文档漂移按运行配置复核，不改名不修拼写） */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 处理车辆生产事件（Kafka 增量）
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleVehicleProduceEvent(String messageJson) {
        try {
            JsonNode root = objectMapper.readTree(messageJson);

            String eventId = root.path("eventId").asText("");
            String eventType = root.path("eventType").asText("");
            String aggregateId = root.path("aggregateId").asText("");
            long version = root.path("version").asLong(0);
            String occurredAt = root.path("occurredAt").asText("");

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

            VehicleProjectionPo existing = vehicleProjectionRepository.findByVinForUpdate(vin).orElse(null);

            if (existing != null && existing.getSourceVersion() != null && existing.getSourceVersion() >= version) {
                log.info("跳过旧版本车辆生产事件: vin={}, existingVersion={}, incomingVersion={}", vin, existing.getSourceVersion(), version);
                return;
            }

            VehicleProjectionPo po = existing != null ? existing : new VehicleProjectionPo();
            po.setVin(vin);
            // production_time NOT NULL 且上游必填：缺失/解析失败拒绝写入，不伪造当前时间（CR-021 §4.25.3）
            po.setProductionTime(parseRequiredDateTime(payload.path("produceTime").asText(""), "produceTime", businessZoneId));
            po.setPlantCode(payload.path("plantCode").asText(null));
            po.setBrandCode(payload.path("brandCode").asText(null));
            po.setPlatformCode(payload.path("platformCode").asText(null));
            po.setCarLineCode(payload.path("carLineCode").asText(null));
            po.setModelCode(payload.path("modelCode").asText(null));
            po.setVariantCode(payload.path("variantCode").asText(null));
            po.setConfigurationCode(payload.path("configurationCode").asText(null));
            po.setSourceEventId(eventId);
            po.setSourceVersion(version);
            // source_event_time NOT NULL 且 occurredAt 为必填信封字段：缺失/解析失败拒绝写入
            po.setSourceEventTime(parseRequiredDateTime(occurredAt, "occurredAt", businessZoneId));
            po.setLastSyncTime(new Date());

            upsert(po, existing != null);
        } catch (Exception e) {
            log.error("处理车辆生产事件失败: {}", e.getMessage(), e);
            throw new RuntimeException("处理车辆生产事件失败", e);
        }
    }

    /**
     * 按 VIN 回源查询并同步车辆投影（CR-015 §4.2）
     * <p>本地缺失 → VMD 回源；NOT_FOUND（明确不存在）与 UNAVAILABLE（服务不可用）严格区分，
     * 服务不可用不得缓存为 NOT_FOUND。</p>
     *
     * @param vin 车架号
     * @return 回源后投影；VMD 明确不存在返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public VehicleProjectionPo syncByVin(String vin) {
        VmdVehicleProjectionDto remote;
        try {
            remote = vmdVehicleProjectionGateway.getByVin(vin);
        } catch (VehicleProjectionNotFoundException e) {
            log.warn("按VIN回源：VMD明确不存在车辆[{}]", vin);
            return null;
        } catch (RetryableProjectionException e) {
            // 服务不可用：抛给调用方受控重试，不缓存为 NOT_FOUND
            throw e;
        }
        if (remote == null) {
            log.warn("按VIN回源：VMD返回空，视为不存在车辆[{}]", vin);
            return null;
        }

        validateRequired(remote, vin);

        VehicleProjectionPo po = toProjection(remote);
        boolean inserted = upsert(po, vehicleProjectionRepository.findByVin(vin).isPresent());
        log.info("按VIN回源同步车辆投影完成: vin={}, sourceVersion={}, inserted={}", vin, po.getSourceVersion(), inserted);
        return vehicleProjectionRepository.findByVin(vin).orElse(po);
    }

    /**
     * 游标快照 Bootstrap（CR-015 §4.3）：从 checkpoint 断点分页拉取，可与 Kafka Consumer 并行。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectionSyncStats bootstrapFromSnapshot() {
        long start = System.currentTimeMillis();
        Checkpoint checkpoint = readCheckpoint();
        String cursor = checkpoint != null ? checkpoint.cursor : null;
        long updatedAfter = checkpoint != null ? checkpoint.updatedAfter : 0L;

        Counter counter = new Counter();

        int page = 0;
        while (page < bootstrapMaxPages) {
            VmdVehicleProjectionSnapshotDto snapshot;
            try {
                snapshot = vmdVehicleProjectionGateway.getSnapshot(cursor, bootstrapPageSize, updatedAfter);
            } catch (RetryableProjectionException e) {
                log.error("Bootstrap 拉取快照失败（服务不可用），保留 checkpoint 断点: cursor={}", cursor, e);
                break;
            }
            if (snapshot == null || snapshot.getItems() == null || snapshot.getItems().isEmpty()) {
                log.info("Bootstrap 快照为空，结束: cursor={}", cursor);
                break;
            }
            for (VmdVehicleProjectionDto dto : snapshot.getItems()) {
                counter.scan++;
                apply(dto, counter);
            }
            cursor = snapshot.getNextCursor();
            if (StrUtil.isBlank(cursor) || !Boolean.TRUE.equals(snapshot.getHasMore())) {
                break;
            }
            page++;
        }

        saveCheckpoint(cursor, updatedAfter);
        long durationMs = System.currentTimeMillis() - start;
        ProjectionSyncStats result = counter.toStats(durationMs);
        log.info("车辆投影 Bootstrap 完成: scan={}, add={}, update={}, ignore={}, fail={}, 耗时={}ms",
                result.getScan(), result.getAdded(), result.getUpdated(), result.getIgnored(), result.getFailed(), durationMs);
        return result;
    }

    /**
     * 周期性对账（CR-015 §4.3）：按 updatedAfter 增量扫描，记录 scan/add/update/ignore/fail、
     * 最大版本差与同步延迟。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectionSyncStats reconcile() {
        long start = System.currentTimeMillis();
        Checkpoint checkpoint = readCheckpoint();
        long updatedAfter = checkpoint != null ? checkpoint.updatedAfter : 0L;

        Counter counter = new Counter();

        String cursor = null;
        int page = 0;
        while (page < bootstrapMaxPages) {
            VmdVehicleProjectionSnapshotDto snapshot;
            try {
                snapshot = vmdVehicleProjectionGateway.getSnapshot(cursor, bootstrapPageSize, updatedAfter);
            } catch (RetryableProjectionException e) {
                log.error("对账拉取快照失败（服务不可用），本次中断: cursor={}", cursor, e);
                break;
            }
            if (snapshot == null || snapshot.getItems() == null || snapshot.getItems().isEmpty()) {
                break;
            }
            for (VmdVehicleProjectionDto dto : snapshot.getItems()) {
                counter.scan++;
                apply(dto, counter);
            }
            cursor = snapshot.getNextCursor();
            if (StrUtil.isBlank(cursor) || !Boolean.TRUE.equals(snapshot.getHasMore())) {
                break;
            }
            page++;
        }

        long now = System.currentTimeMillis();
        saveCheckpoint(cursor, now);
        ProjectionSyncStats result = counter.toStats(now - start);
        log.info("车辆投影对账完成: scan={}, add={}, update={}, ignore={}, fail={}, maxVersionDiff={}, 耗时={}ms",
                result.getScan(), result.getAdded(), result.getUpdated(), result.getIgnored(),
                result.getFailed(), result.getMaxVersionDiff(), result.getSyncDelayMs());
        return result;
    }

    // ==================== 内部工具 ====================

    /** 投影 upsert（带 source_version 条件，防旧覆盖新） */
    private void apply(VmdVehicleProjectionDto dto, Counter counter) {
        try {
            if (dto == null || StrUtil.isBlank(dto.getVin())) {
                counter.failed++;
                return;
            }
            VehicleProjectionPo po = toProjection(dto);
            VehicleProjectionPo local = vehicleProjectionRepository.findByVin(dto.getVin()).orElse(null);
            if (local == null) {
                vehicleProjectionRepository.insert(po);
                counter.added++;
            } else {
                boolean updated = vehicleProjectionRepository.updateIfNewerVersion(po);
                if (updated) {
                    counter.updated++;
                } else {
                    counter.ignored++;
                    long diff = Math.abs(Optional.ofNullable(local.getSourceVersion()).orElse(0L)
                            - Optional.ofNullable(po.getSourceVersion()).orElse(0L));
                    if (diff > counter.maxVersionDiff) {
                        counter.maxVersionDiff = diff;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("对账/回源 upsert 失败: vin={}, error={}", dto != null ? dto.getVin() : "null", e.getMessage());
            counter.failed++;
        }
    }

    /** 插入或按版本更新（并发安全） */
    private boolean upsert(VehicleProjectionPo po, boolean exists) {
        if (!exists) {
            po.setCreateTime(new Date());
            po.setModifyTime(new Date());
            try {
                vehicleProjectionRepository.insert(po);
                return true;
            } catch (DuplicateKeyException e) {
                // 并发插入冲突，回退到更新
                VehicleProjectionPo existing = vehicleProjectionRepository.findByVinForUpdate(po.getVin()).orElse(null);
                if (existing != null) {
                    return vehicleProjectionRepository.updateIfNewerVersion(po);
                }
                return true;
            }
        }
        return vehicleProjectionRepository.updateIfNewerVersion(po);
    }

    /** 校验必填字段（CR-015 §4.2：validate vin and required fields） */
    private void validateRequired(VmdVehicleProjectionDto remote, String vin) {
        if (remote.getVin() == null || !remote.getVin().equals(vin)) {
            throw new IllegalStateException("VMD 回源 VIN 不一致: requested=" + vin + ", returned=" + remote.getVin());
        }
        if (remote.getSourceVersion() == null || remote.getSourceVersion() <= 0) {
            throw new IllegalStateException("VMD 回源缺少 sourceVersion: vin=" + vin);
        }
        if (StrUtil.isBlank(remote.getConfigurationCode())) {
            // configuration_code 为核心锚点，缺失告警但允许落库（生产时间可为兜底）
            log.warn("VMD 回源缺少 configurationCode: vin={}", vin);
        }
        if (StrUtil.isBlank(remote.getProductionTime())) {
            // production_time NOT NULL：必填缺失拒绝本次写入（CR-021 §4.25.3，不伪造当前时间）
            throw new IllegalStateException("VMD 回源缺少 productionTime: vin=" + vin);
        }
    }

    private VehicleProjectionPo toProjection(VmdVehicleProjectionDto dto) {
        VehicleProjectionPo po = new VehicleProjectionPo();
        po.setVin(dto.getVin());
        // production_time NOT NULL：缺失/解析失败拒绝写入（apply() 捕获后计入 failed，走现有隔离机制）
        po.setProductionTime(parseRequiredDateTime(dto.getProductionTime(), "productionTime", businessZoneId));
        po.setPlantCode(dto.getPlantCode());
        po.setBrandCode(dto.getBrandCode());
        po.setPlatformCode(dto.getPlatformCode());
        po.setCarLineCode(dto.getCarLineCode());
        po.setModelCode(dto.getModelCode());
        po.setVariantCode(dto.getVariantCode());
        po.setConfigurationCode(dto.getConfigurationCode());
        po.setSourceEventId("VMD-REFSRC");
        po.setSourceVersion(dto.getSourceVersion() != null ? dto.getSourceVersion() : 0L);
        po.setSourceEventTime(new Date());
        po.setLastSyncTime(new Date());
        return po;
    }

    // ==================== Checkpoint（tb_data_sync_record）====================

    private Checkpoint readCheckpoint() {
        Map<String, Object> query = new HashMap<>();
        query.put("source", CHECKPOINT_SOURCE);
        query.put("type", CHECKPOINT_TYPE);
        query.put("code", CHECKPOINT_CODE);
        List<DataSyncRecordPo> records = dataSyncRecordMapper.selectPoByMap(query);
        if (records == null || records.isEmpty()) {
            return null;
        }
        try {
            JSONObject json = JSONUtil.parseObj(records.get(0).getData());
            return new Checkpoint(json.getStr("cursor"), json.getLong("updatedAfter", 0L));
        } catch (Exception e) {
            log.warn("解析投影同步 checkpoint 失败: {}", e.getMessage());
            return null;
        }
    }

    private void saveCheckpoint(String cursor, long updatedAfter) {
        JSONObject json = new JSONObject();
        json.set("cursor", cursor);
        json.set("updatedAfter", updatedAfter);
        Map<String, Object> query = new HashMap<>();
        query.put("source", CHECKPOINT_SOURCE);
        query.put("type", CHECKPOINT_TYPE);
        query.put("code", CHECKPOINT_CODE);
        List<DataSyncRecordPo> records = dataSyncRecordMapper.selectPoByMap(query);
        if (records == null || records.isEmpty()) {
            DataSyncRecordPo po = new DataSyncRecordPo();
            po.setSource(CHECKPOINT_SOURCE);
            po.setType(CHECKPOINT_TYPE);
            po.setCode(CHECKPOINT_CODE);
            po.setData(json.toString());
            po.setState(1);
            dataSyncRecordMapper.insertPo(po);
        } else {
            DataSyncRecordPo po = records.get(0);
            po.setData(json.toString());
            dataSyncRecordMapper.updatePo(po);
        }
        log.debug("保存投影同步 checkpoint: cursor={}, updatedAfter={}", cursor, updatedAfter);
    }

    /**
     * 解析 VMD 事件/快照中的必填时间字段（CR-021 §4.25.3）。
     * <p>规则：
     * <ul>
     *   <li>blank：必填字段缺失 → 抛异常拒绝本次写入，不伪造当前时间；</li>
     *   <li>带 offset/Z 的 ISO 串：绝对时刻，直接转换；</li>
     *   <li>无时区墙上时间串：按显式业务时区（businessZoneId）解释，不用 systemDefault 猜测；</li>
     *   <li>非空但解析失败 → 抛异常，走现有失败/隔离机制，不伪装成正常缺失。</li>
     * </ul>
     *
     * @param dateTimeStr 原始时间串
     * @param fieldName   字段名（错误信息用）
     * @param zoneId      无时区串的显式解释时区
     */
    static Date parseRequiredDateTime(String dateTimeStr, String fieldName, ZoneId zoneId) {
        if (StrUtil.isBlank(dateTimeStr)) {
            throw new IllegalStateException("车辆投影必填时间字段[" + fieldName + "]缺失，拒绝写入");
        }
        String candidate = dateTimeStr.trim();
        try {
            return Date.from(OffsetDateTime.parse(candidate).toInstant());
        } catch (DateTimeParseException ignored) {
            // 无时区墙上时间串：继续按显式业务时区解释
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(candidate, DATE_TIME_FORMATTER);
            return Date.from(localDateTime.atZone(zoneId).toInstant());
        } catch (DateTimeParseException e1) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(candidate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return Date.from(localDateTime.atZone(zoneId).toInstant());
            } catch (DateTimeParseException e2) {
                throw new IllegalStateException(
                        "车辆投影时间字段[" + fieldName + "]解析失败，拒绝写入: " + candidate, e2);
            }
        }
    }

    /** Bootstrap/对账统计结果 */
    @Getter
    @Builder
    public static class ProjectionSyncStats {
        private int scan;
        private int added;
        private int updated;
        private int ignored;
        private int failed;
        private long maxVersionDiff;
        private long syncDelayMs;
    }

    /** 本地可变计数器（统计用） */
    private static final class Counter {
        int scan = 0;
        int added = 0;
        int updated = 0;
        int ignored = 0;
        int failed = 0;
        long maxVersionDiff = 0L;

        ProjectionSyncStats toStats(long durationMs) {
            return ProjectionSyncStats.builder()
                    .scan(scan)
                    .added(added)
                    .updated(updated)
                    .ignored(ignored)
                    .failed(failed)
                    .maxVersionDiff(maxVersionDiff)
                    .syncDelayMs(durationMs)
                    .build();
        }
    }

    private record Checkpoint(String cursor, long updatedAfter) {
    }
}
