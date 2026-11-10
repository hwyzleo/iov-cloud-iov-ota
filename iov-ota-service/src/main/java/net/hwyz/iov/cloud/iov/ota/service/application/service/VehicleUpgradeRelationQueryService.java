package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.VehicleRelationTaskQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleActivityRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleUpgradeRelationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆升级关联查询服务（IOV-OTA-DSN-CR-023 §4.1 / §4.2）
 *
 * <p>以已生成的 VehicleTask 为唯一关联依据（RD-023-2）：
 * <ul>
 *   <li>活动：一次分组查询返回去重活动、任务数与最近任务时间，分页总数为活动数；</li>
 *   <li>任务：一次 JOIN 查询返回任务/活动名称与状态，禁止逐行补全（RD-023-6）。</li>
 * </ul>
 *
 * <p>分页由 Controller 层 startPage() 开启（调用本服务方法前），本服务不做内存分页。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleUpgradeRelationQueryService {

    private final VehicleUpgradeRelationMapper vehicleUpgradeRelationMapper;

    /**
     * 查询某 VIN 关联的升级活动（同一活动仅一条，按最近任务时间倒序）。
     *
     * @param vin 车架号
     * @return 活动关联结果列表（已分页，若 Controller 开启分页）
     */
    public List<VehicleActivityRelationResult> listActivities(String vin) {
        return vehicleUpgradeRelationMapper.selectActivityRelations(vin);
    }

    /**
     * 查询某 VIN 关联的升级任务（全部历史，默认创建时间倒序，支持过滤）。
     *
     * @param query 查询条件（vin 可选；为空时返回全部车辆任务，供管理端全局列表使用）
     * @return 任务关联结果列表（已分页，若 Controller 开启分页）
     */
    public List<VehicleTaskRelationResult> listTasks(VehicleRelationTaskQuery query) {
        return vehicleUpgradeRelationMapper.selectTaskRelations(query);
    }
}
