package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Task 级授权查询结果（CR-016 §6/§8）
 *
 * <p>授权汇总以 tb_task_vehicle.consent_state 聚合，避免每次扫描全部历史；
 * 历史详情才读取 tb_vehicle_task_consent。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskConsentQueryResult {

    private Long taskId;

    /** 授权状态分布：consent_state -> 车辆数 */
    private Map<String, Long> consentStateSummary;

    /** 车辆授权列表（当前状态 + 当前 receipt + 条款版本 + 无效原因） */
    private List<TaskConsentVehicleView> vehicles;
}
