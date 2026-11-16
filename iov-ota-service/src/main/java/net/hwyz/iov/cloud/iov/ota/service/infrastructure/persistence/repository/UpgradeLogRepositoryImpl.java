package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradeLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.UpgradeLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.UpgradeLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradeLogPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * UpgradeLog Repository 实现（IOV-OTA-DSN-CR-026 §6.5）。
 *
 * <p>以对象键为事实锚点，保留历史 task_id/vin 兼容列；不物理删除历史日志登记。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UpgradeLogRepositoryImpl implements UpgradeLogRepository {

    private final UpgradeLogMapper mapper;

    @Override
    public Optional<UpgradeLog> getById(Long id) {
        UpgradeLogPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public Optional<UpgradeLog> findByLogRequestId(String logRequestId) {
        if (logRequestId == null || logRequestId.isBlank()) {
            return Optional.empty();
        }
        UpgradeLogPo po = mapper.selectOne(new QueryWrapper<UpgradeLogPo>()
                .eq("log_request_id", logRequestId)
                .eq("row_valid", true)
                .last("LIMIT 1"));
        return Optional.ofNullable(po == null ? null : toDomain(po));
    }

    @Override
    public Optional<UpgradeLog> findByObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return Optional.empty();
        }
        UpgradeLogPo po = mapper.selectOne(new QueryWrapper<UpgradeLogPo>()
                .eq("object_key", objectKey)
                .eq("row_valid", true)
                .last("LIMIT 1"));
        return Optional.ofNullable(po == null ? null : toDomain(po));
    }

    @Override
    public List<UpgradeLog> listByVehicleTask(Long vehicleTaskId, Instant beginTime, Instant endTime,
                                             String uploadState) {
        QueryWrapper<UpgradeLogPo> query = new QueryWrapper<UpgradeLogPo>()
                .eq("vehicle_task_id", vehicleTaskId)
                .eq("row_valid", true);
        if (beginTime != null) {
            query.ge("authorized_at", LocalDateTime.ofInstant(beginTime, ZoneId.systemDefault()));
        }
        if (endTime != null) {
            query.le("authorized_at", LocalDateTime.ofInstant(endTime, ZoneId.systemDefault()));
        }
        if (uploadState != null && !uploadState.isBlank()) {
            query.eq("upload_state", uploadState);
        }
        query.orderByDesc("authorized_at").orderByDesc("id");
        return mapper.selectList(query).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public UpgradeLog save(UpgradeLog entity) {
        UpgradeLogPo po = toPo(entity);
        if (entity.getId() == null) {
            mapper.insert(po);
            entity.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return entity;
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private UpgradeLog toDomain(UpgradeLogPo po) {
        return UpgradeLog.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .vin(po.getVin())
                .vehicleTaskId(po.getVehicleTaskId())
                .executionId(po.getExecutionId())
                .logRequestId(po.getLogRequestId())
                .logScope(po.getLogScope())
                .desensitizeVersion(po.getDesensitizeVersion())
                .privacyLevel(po.getPrivacyLevel())
                .objectKey(po.getObjectKey())
                .logObjectId(po.getLogObjectId())
                .objectSizeBytes(po.getObjectSizeBytes())
                .logDigest(po.getLogDigest())
                .digestAlgorithm(po.getDigestAlgorithm())
                .uploadState(po.getUploadState())
                .uploadResult(po.getUploadResult())
                .failureReason(po.getFailureReason())
                .logUrl(po.getLogUrl())
                .authorizedAt(toInstant(po.getAuthorizedAt()))
                .uploadedAt(toInstant(po.getUploadedAt()))
                .receivedAt(toInstant(po.getReceivedAt()))
                .uploadTime(toInstant(po.getUploadTime()))
                .build();
    }

    private UpgradeLogPo toPo(UpgradeLog domain) {
        UpgradeLogPo po = new UpgradeLogPo();
        po.setId(domain.getId());
        po.setTaskId(domain.getTaskId());
        po.setVin(domain.getVin());
        po.setVehicleTaskId(domain.getVehicleTaskId());
        po.setExecutionId(domain.getExecutionId());
        po.setLogRequestId(domain.getLogRequestId());
        po.setLogScope(domain.getLogScope());
        po.setDesensitizeVersion(domain.getDesensitizeVersion());
        po.setPrivacyLevel(domain.getPrivacyLevel());
        po.setObjectKey(domain.getObjectKey());
        po.setLogObjectId(domain.getLogObjectId());
        po.setObjectSizeBytes(domain.getObjectSizeBytes());
        po.setLogDigest(domain.getLogDigest());
        po.setDigestAlgorithm(domain.getDigestAlgorithm());
        po.setUploadState(domain.getUploadState());
        po.setUploadResult(domain.getUploadResult());
        po.setFailureReason(domain.getFailureReason());
        po.setLogUrl(domain.getLogUrl());
        po.setAuthorizedAt(toDate(domain.getAuthorizedAt()));
        po.setUploadedAt(toDate(domain.getUploadedAt()));
        po.setReceivedAt(toDate(domain.getReceivedAt()));
        po.setUploadTime(toDate(domain.getUploadTime()));
        po.setCreateTime(domain.getAuthorizedAt() == null ? new Date() : toDate(domain.getAuthorizedAt()));
        po.setModifyTime(new Date());
        po.setRowValid(true);
        po.setDescription("CR-026 日志对象登记");
        return po;
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }
}
