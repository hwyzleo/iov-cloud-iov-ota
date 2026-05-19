package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartHistoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆零件数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehiclePartPoConverter {

    VehiclePartPoConverter INSTANCE = Mappers.getMapper(VehiclePartPoConverter.class);

    /**
     * 数据对象转历史数据对象
     *
     * @param vehiclePartPo 数据对象
     * @return 历史数据对象
     */
    @Mappings({})
    VehiclePartHistoryPo toHistory(VehiclePartPo vehiclePartPo);

    /**
     * 数据对象列表转历史数据对象列表
     *
     * @param vehiclePartPoList 数据对象列表
     * @return 历史数据对象列表
     */
    List<VehiclePartHistoryPo> toHistoryList(List<VehiclePartPo> vehiclePartPoList);

}
