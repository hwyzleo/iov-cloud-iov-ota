package net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto.VmdVehicleProjectionDto;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto.VmdVehicleProjectionSnapshotDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * VMD 车辆主档查询网关（CR-015 §4.1）
 * <p>Feign 客户端，指向 edd-vmd /api/service/vehicle/v1；
 * 按 VIN 回源与游标快照（Bootstrap/对账共用）。
 * 命名避免使用 OtaVehicleService（防止与 OTA 对外契约混淆）。</p>
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "vmdVehicleProjectionGateway", value = ServiceNameConstants.EDD_VMD,
        path = "/api/service/vehicle/v1", fallbackFactory = VmdVehicleProjectionFallbackFactory.class)
public interface VmdVehicleProjectionGateway {

    /**
     * 按 VIN 回源查询车辆主档
     * @param vin 车架号
     * @return 车辆主档投影字段（VMD 明确不存在时降级抛 VehicleProjectionNotFoundException）
     */
    @GetMapping(value = "/{vin}")
    VmdVehicleProjectionDto getByVin(@PathVariable("vin") String vin);

    /**
     * 游标快照分页拉取（按更新时间增量）
     * @param cursor       游标
     * @param size         页大小
     * @param updatedAfter 仅返回更新时间之后的数据（毫秒时间戳，可空）
     * @return 快照页
     */
    @GetMapping(value = "/snapshot")
    VmdVehicleProjectionSnapshotDto getSnapshot(@RequestParam("cursor") String cursor,
                                                @RequestParam("size") Integer size,
                                                @RequestParam(value = "updatedAfter", required = false) Long updatedAfter);
}
