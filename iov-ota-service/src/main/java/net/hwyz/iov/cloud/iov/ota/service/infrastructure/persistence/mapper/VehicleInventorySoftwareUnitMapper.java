package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventorySoftwareUnitPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 车辆 ECU 清单软件单元 DAO（CR-019 §5.3）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleInventorySoftwareUnitMapper extends BaseMapper<VehicleInventorySoftwareUnitPo> {

    @Select("SELECT * FROM tb_vehicle_inventory_software_unit WHERE inventory_item_id = #{inventoryItemId} AND row_valid = 1 "
            + "ORDER BY software_target_code, slot_key")
    List<VehicleInventorySoftwareUnitPo> selectByInventoryItemId(@Param("inventoryItemId") Long inventoryItemId);

    @Select("SELECT * FROM tb_vehicle_inventory_software_unit WHERE inventory_id = #{inventoryId} AND row_valid = 1 "
            + "ORDER BY ecu_id, software_target_code, slot_key")
    List<VehicleInventorySoftwareUnitPo> selectByInventoryId(@Param("inventoryId") Long inventoryId);

    @Select("SELECT * FROM tb_vehicle_inventory_software_unit WHERE vin = #{vin} AND row_valid = 1 "
            + "AND inventory_id = #{inventoryId} ORDER BY ecu_id, software_target_code, slot_key")
    List<VehicleInventorySoftwareUnitPo> selectByVinAndInventoryId(@Param("vin") String vin,
                                                                   @Param("inventoryId") Long inventoryId);
}
