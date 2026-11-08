package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryReportStatePo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

/**
 * OTA 车辆软件清单上报运行态 DAO（CR-022 §4）
 *
 * <p>每 VIN 一行，仅提供单调 upsert 与只读查询；不提供管理端写接口。
 * 运行态写入必须与对应业务处理处于同一本地事务（RD-022-4）。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Mapper
public interface VehicleInventoryReportStateMapper {

    /**
     * 单调 upsert 最后上报时间：同 VIN 并发上报时，较旧 reportedAt 不得覆盖较新值（CR-022 §4.3）。
     *
     * @param vin        车架号
     * @param reportedAt 本次服务端受理时刻
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_vehicle_inventory_report_state "
            + "(vin, last_reported_at, create_time, modify_time, row_version, row_valid) "
            + "VALUES (#{vin}, #{reportedAt}, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 1) "
            + "ON DUPLICATE KEY UPDATE "
            + "last_reported_at = GREATEST(last_reported_at, #{reportedAt}), "
            + "modify_time = CURRENT_TIMESTAMP(3), "
            + "row_version = row_version + 1, "
            + "row_valid = 1")
    int upsertReported(@Param("vin") String vin, @Param("reportedAt") Date reportedAt);

    /**
     * 根据车架号查询运行态。
     *
     * @param vin 车架号
     * @return 运行态，不存在返回 null
     */
    @Select("SELECT * FROM tb_vehicle_inventory_report_state WHERE vin = #{vin} AND row_valid = 1 LIMIT 1")
    VehicleInventoryReportStatePo selectByVin(@Param("vin") String vin);
}
