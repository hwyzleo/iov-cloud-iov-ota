package net.hwyz.iov.cloud.ota.pota.api.feign.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.ota.pota.api.contract.request.SaveVehiclePartsRequest;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.factory.ExVehiclePartServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 车辆零件相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exVehiclePartService", value = ServiceNameConstants.OTA_POTA, path = "/service/vehiclePart", fallbackFactory = ExVehiclePartServiceFallbackFactory.class)
public interface ExVehiclePartService {

    /**
     * 保存车辆零部件信息
     *
     * @param request 保存车辆零部件信息请求
     */
    @PutMapping("/{vin}/action/saveParts")
    void saveVehicleParts(@PathVariable("vin") String vin, @RequestBody @Validated SaveVehiclePartsRequest request);

}
