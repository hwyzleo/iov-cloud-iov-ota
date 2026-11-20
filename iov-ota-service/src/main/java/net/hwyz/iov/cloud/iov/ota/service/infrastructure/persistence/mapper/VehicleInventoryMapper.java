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

    /**
     * 取该 VIN 最近一次“已受理”的清单头。
     *
     * <p>按 {@code accepted_time}（受理时间）而非 {@code inventory_revision} 取最新：
     * 车端本地状态清空或换件后 revision 会从 1 重新分配（CGW-FOTA CR-012），此时历史
     * 行可能带有更大的 revision，但业务上的“当前清单”仍是最近被受理的那一行。
     * revision 正常单调递增时两种排序等价。
     */
    @Select("SELECT * FROM tb_vehicle_inventory WHERE vin = #{vin} AND row_valid = 1 "
            + "ORDER BY accepted_time DESC, id DESC LIMIT 1")
    VehicleInventoryPo selectLatestByVin(@Param("vin") String vin);

    @Select("SELECT * FROM tb_vehicle_inventory WHERE vin = #{vin} AND inventory_revision = #{revision} AND row_valid = 1 LIMIT 1")
    VehicleInventoryPo selectByVinAndRevision(@Param("vin") String vin, @Param("revision") Long revision);
}
