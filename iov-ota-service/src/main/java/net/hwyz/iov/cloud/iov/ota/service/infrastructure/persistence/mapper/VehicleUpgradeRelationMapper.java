package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.VehicleRelationTaskQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleActivityRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskRelationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 车辆升级关联查询 DAO（IOV-OTA-DSN-CR-023 §4.1 / §4.2）
 *
 * <p>以 tb_task_vehicle 为关联起点，一次 JOIN tb_task / tb_activity 取回名称与状态，
 * 禁止 Controller / Service 逐行补数据。分页由 PageHelper 在 Controller 层开启。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Mapper
public interface VehicleUpgradeRelationMapper {

    /**
     * 按 VIN 分组查询关联活动（同一活动仅一行）。
     *
     * @param vin 车架号
     * @return 活动关联结果（vehicleTaskCount / latestTaskTime 已聚合）
     */
    List<VehicleActivityRelationResult> selectActivityRelations(@Param("vin") String vin);

    /**
     * 按 VIN 分页查询关联任务（已发布生成的 VehicleTask，全部历史）。
     *
     * @param query 查询条件（vin 可选；为空时返回全部车辆任务）
     * @return 任务关联结果（task/activity 名称与状态由 JOIN 一次取得）
     */
    List<VehicleTaskRelationResult> selectTaskRelations(VehicleRelationTaskQuery query);
}
