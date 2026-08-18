package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskConsentQueryResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskConsentVehicleView;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ConsentPolicy;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Task 级授权运营查询服务（CR-016 §6/§8、US-102～105）
 *
 * <p>授权汇总以 tb_task_vehicle.consent_state 聚合；车辆分页返回 VehicleTask
 * 当前状态、当前 receipt、条款版本与无效原因。管理查询是同一事实模型的只读投影，
 * 不创建第二张“管理授权表”。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskConsentQueryService {

    private final VehicleTaskMapper vehicleTaskMapper;
    private final VehicleTaskRepository vehicleTaskRepository;
    private final VehicleTaskConsentRepository vehicleTaskConsentRepository;
    private final ConsentPolicy consentPolicy;

    /**
     * Task 授权汇总 + 车辆分页。
     *
     * @param taskId     任务ID
     * @param state      授权状态过滤（可空）
     * @param vin        VIN 过滤（可空）
     * @param beginTime  接收时间下限（可空）
     * @param endTime    接收时间上限（可空）
     */
    public TaskConsentQueryResult queryTaskConsents(Long taskId, String state, String vin,
                                                    Date beginTime, Date endTime) {
        // 1. 车辆列表（支持分页，由 Controller startPage 生效；作为本线程首个查询被分页）
        QueryWrapper<TaskVehiclePo> query = new QueryWrapper<TaskVehiclePo>()
                .eq("task_id", taskId)
                .eq("row_valid", 1);
        if (state != null && !state.isBlank()) {
            query.eq("consent_state", state);
        }
        if (vin != null && !vin.isBlank()) {
            query.eq("vin", vin);
        }
        if (beginTime != null) {
            query.ge("consent_updated_at", beginTime);
        }
        if (endTime != null) {
            query.le("consent_updated_at", endTime);
        }
        query.orderByDesc("consent_updated_at").orderByDesc("id");

        List<TaskConsentVehicleView> vehicles = vehicleTaskMapper.selectList(query)
                .stream()
                .map(this::toVehicleView)
                .collect(Collectors.toList());

        // 2. 状态分布聚合（避免扫描全部历史）
        Map<String, Long> summary = new HashMap<>();
        for (Map<String, Object> row : vehicleTaskMapper.countConsentStateByTask(taskId)) {
            Object stateVal = row.get("state");
            Object cntVal = row.get("cnt");
            summary.put(stateVal != null ? String.valueOf(stateVal) : "NULL",
                    cntVal != null ? ((Number) cntVal).longValue() : 0L);
        }

        return TaskConsentQueryResult.builder()
                .taskId(taskId)
                .consentStateSummary(summary)
                .vehicles(vehicles)
                .build();
    }

    private TaskConsentVehicleView toVehicleView(TaskVehiclePo po) {
        VehicleTask vt = vehicleTaskRepository.getById(
                net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId.of(po.getId()))
                .orElse(null);
        Optional<VehicleTaskConsent> current = vehicleTaskConsentRepository
                .findCurrentByVehicleTaskId(po.getId());
        VehicleTaskConsent currentConsent = current.orElse(null);
        Instant now = Instant.now();

        String invalidReason = null;
        if (vt != null) {
            invalidReason = consentPolicy.invalidReason(vt, currentConsent, now);
        }
        return TaskConsentVehicleView.builder()
                .vehicleTaskId(po.getId())
                .vinMasked(maskVin(po.getVin()))
                .consentState(po.getConsentState())
                .currentReceiptId(currentConsent != null ? currentConsent.getConsentReceiptId() : null)
                .articleVersion(currentConsent != null ? currentConsent.getArticleVersion() : null)
                .consentUpdatedAt(toInstant(po.getConsentUpdatedAt()))
                .invalidReason(invalidReason)
                .build();
    }

    private static String maskVin(String vin) {
        if (vin == null || vin.isBlank() || vin.length() <= 4) {
            return "***";
        }
        return "***" + vin.substring(vin.length() - 4);
    }

    private static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    private static Instant toInstant(java.time.LocalDateTime ldt) {
        return ldt != null ? ldt.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
