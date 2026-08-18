package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.VehicleTaskConsentConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskConsentMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskConsentPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * VehicleTask 授权事实仓储实现（CR-016）
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleTaskConsentRepositoryImpl implements VehicleTaskConsentRepository {

    private final VehicleTaskConsentMapper vehicleTaskConsentMapper;
    private final VehicleTaskMapper vehicleTaskMapper;
    private final VehicleTaskConsentConverter converter;

    @Override
    public VehicleTaskConsent append(VehicleTaskConsent consent) {
        VehicleTaskConsentPo po = converter.toPo(consent);
        vehicleTaskConsentMapper.insert(po);
        consent.setId(po.getId());
        log.info("授权历史已追加，记录[{}]，车辆任务[{}]，结果[{}]",
                po.getId(), po.getVehicleTaskId(), po.getConsentResult());
        return consent;
    }

    @Override
    public Optional<VehicleTaskConsent> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        VehicleTaskConsentPo po = vehicleTaskConsentMapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid())
                ? converter.toDomain(po) : null);
    }

    @Override
    public Optional<VehicleTaskConsent> findByMessageId(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(converter.toDomain(vehicleTaskConsentMapper.selectByMessageId(messageId)));
    }

    @Override
    public Optional<VehicleTaskConsent> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(converter.toDomain(vehicleTaskConsentMapper.selectByIdempotencyKey(idempotencyKey)));
    }

    @Override
    public Optional<VehicleTaskConsent> findByReceiptId(String consentReceiptId) {
        if (consentReceiptId == null || consentReceiptId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(converter.toDomain(vehicleTaskConsentMapper.selectByReceiptId(consentReceiptId)));
    }

    @Override
    public Optional<VehicleTaskConsent> findCurrentByVehicleTaskId(Long vehicleTaskId) {
        TaskVehiclePo vtPo = vehicleTaskMapper.selectById(vehicleTaskId);
        if (vtPo == null || vtPo.getCurrentConsentId() == null) {
            return Optional.empty();
        }
        return findById(vtPo.getCurrentConsentId());
    }

    @Override
    public List<VehicleTaskConsent> findByVehicleTaskId(Long vehicleTaskId) {
        QueryWrapper<VehicleTaskConsentPo> query = new QueryWrapper<VehicleTaskConsentPo>()
                .eq("vehicle_task_id", vehicleTaskId)
                .eq("row_valid", 1)
                .orderByAsc("received_at").orderByAsc("id");
        return converter.toDomainList(vehicleTaskConsentMapper.selectList(query));
    }

    @Override
    public List<VehicleTaskConsent> findByTaskId(Long taskId, String consentResult,
                                                 Instant beginTime, Instant endTime) {
        QueryWrapper<VehicleTaskConsentPo> query = new QueryWrapper<VehicleTaskConsentPo>()
                .eq("task_id", taskId)
                .eq("row_valid", 1);
        if (consentResult != null && !consentResult.isBlank()) {
            query.eq("consent_result", consentResult);
        }
        if (beginTime != null) {
            query.ge("received_at", Date.from(beginTime));
        }
        if (endTime != null) {
            query.le("received_at", Date.from(endTime));
        }
        query.orderByDesc("received_at").orderByDesc("id");
        return converter.toDomainList(vehicleTaskConsentMapper.selectList(query));
    }
}
