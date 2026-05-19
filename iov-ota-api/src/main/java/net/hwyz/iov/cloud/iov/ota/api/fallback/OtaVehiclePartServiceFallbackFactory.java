package net.hwyz.iov.cloud.iov.ota.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.request.SaveVehiclePartsRequest;
import net.hwyz.iov.cloud.iov.ota.api.service.OtaVehiclePartService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆零件相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class OtaVehiclePartServiceFallbackFactory implements FallbackFactory<OtaVehiclePartService> {

    @Override
    public OtaVehiclePartService create(Throwable throwable) {
        return new OtaVehiclePartService() {
            @Override
            public void saveVehicleParts(String vin, SaveVehiclePartsRequest request) {
                log.error("车辆零件相关服务保存车辆[{}]零部件信息调用异常", vin, throwable);
            }
        };
    }
}
