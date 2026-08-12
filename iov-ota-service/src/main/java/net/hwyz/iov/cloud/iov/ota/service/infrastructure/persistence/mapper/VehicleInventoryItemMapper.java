package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 车辆 ECU 清单明细 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleInventoryItemMapper extends BaseMapper<VehicleInventoryItemPo> {

    @Select("SELECT * FROM tb_vehicle_inventory_item WHERE inventory_id = #{inventoryId} AND row_valid = 1")
    List<VehicleInventoryItemPo> selectByInventoryId(@Param("inventoryId") Long inventoryId);
}
