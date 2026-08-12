package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车辆 ECU 清单头 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleInventoryMapper extends BaseMapper<VehicleInventoryPo> {

    @Select("SELECT * FROM tb_vehicle_inventory WHERE vin = #{vin} AND row_valid = 1 ORDER BY inventory_revision DESC LIMIT 1")
    VehicleInventoryPo selectLatestByVin(@Param("vin") String vin);
}
