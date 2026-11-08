package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleMpt;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleManagementResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 升级车辆管理查询结果到管理后台VO转换类
 * <p>
 * CR-011: 支持车辆主档投影查询结果到 VehicleMpt 的转换
 * <p>
 * CR-022: 改为从 VehicleManagementResult（主档投影 + 运行态 lastInventoryReportedAt）组装，
 * 不提供写回 VehicleProjectionPo / tb_vehicle_projection 的转换方法（RD-022-1）。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Mapper
public interface VehicleProjectionMptAssembler {

    VehicleProjectionMptAssembler INSTANCE = Mappers.getMapper(VehicleProjectionMptAssembler.class);

    /**
     * 升级车辆管理查询结果转管理后台数据传输对象
     *
     * @param vehicleManagementResult 管理查询结果
     * @return 管理后台数据传输对象
     */
    VehicleMpt fromResult(VehicleManagementResult vehicleManagementResult);

    /**
     * 升级车辆管理查询结果列表转管理后台数据传输对象列表
     *
     * @param vehicleManagementResultList 管理查询结果列表
     * @return 管理后台数据传输对象列表
     */
    List<VehicleMpt> fromResultList(List<VehicleManagementResult> vehicleManagementResultList);
}
