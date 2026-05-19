package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.request.SaveVehiclePartsRequest;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.VehiclePartExServiceAssembler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 车辆零件相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehiclePart/v1")
public class VehiclePartServiceController {

    private final VehiclePartAppService vehiclePartAppService;

    /**
     * 保存车辆零件信息
     *
     * @param request 保存车辆零件信息请求
     */
    @PutMapping("/{vin}/action/saveParts")
    void saveVehicleParts(@PathVariable("vin") String vin, @RequestBody @Validated SaveVehiclePartsRequest request) {
        log.info("保存车辆[{}]零件信息", vin);
        vehiclePartAppService.saveVehicleParts(vin, request.getRemark(), VehiclePartExServiceAssembler.INSTANCE.toPoList(request.getVehiclePartList()));
    }

}
