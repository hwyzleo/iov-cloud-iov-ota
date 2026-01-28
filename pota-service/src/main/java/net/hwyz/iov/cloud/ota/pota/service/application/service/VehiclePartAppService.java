package net.hwyz.iov.cloud.ota.pota.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.assembler.VehiclePartPoAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.VehiclePartDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.VehiclePartHistoryDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.VehiclePartHistoryPo;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.VehiclePartPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆零件相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehiclePartAppService {

    private final VehiclePartDao vehiclePartDao;
    private final VehiclePartHistoryDao vehiclePartHistoryDao;

    /**
     * 保存车辆零件信息
     *
     * @param vin             车架号
     * @param remark          备注
     * @param vehiclePartList 车辆零件列表
     */
    public void saveVehicleParts(String vin, String remark, List<VehiclePartPo> vehiclePartList) {
        for (VehiclePartPo vehiclePart : vehiclePartList) {
            VehiclePartPo vehiclePartPo = vehiclePartDao.selectPoByVinAndPn(vin, vehiclePart.getPn());
            if (ObjUtil.isNull(vehiclePartPo)) {
                VehiclePartPo newVehPartPo = VehiclePartPo.builder()
                        .vin(vin)
                        .pn(vehiclePart.getPn())
                        .deviceCode(vehiclePart.getDeviceCode())
                        .deviceItem(vehiclePart.getDeviceItem())
                        .sn(vehiclePart.getSn())
                        .configWord(vehiclePart.getConfigWord())
                        .supplierCode(vehiclePart.getSupplierCode())
                        .batchNum(vehiclePart.getBatchNum())
                        .hardwareVer(vehiclePart.getHardwareVer())
                        .softwareVer(vehiclePart.getSoftwareVer())
                        .hardwarePn(vehiclePart.getHardwarePn())
                        .softwarePn(vehiclePart.getSoftwarePn())
                        .extra(vehiclePart.getExtra())
                        .build();
                vehiclePartDao.insertPo(newVehPartPo);
                recordLog(newVehPartPo, StrUtil.nullToEmpty(remark) + "新增");
            } else {
                if (!StrUtil.nullToEmpty(vehiclePartPo.getSn()).equalsIgnoreCase(vehiclePart.getSn())) {
                    String changeRemark = "SN：" + vehiclePartPo.getSn() + "->" + vehiclePart.getSn();
                    vehiclePartPo.setSn(vehiclePart.getSn());
                    vehiclePartDao.updatePo(vehiclePartPo);
                    recordLog(vehiclePartPo, StrUtil.nullToEmpty(remark) + changeRemark);
                }
            }
        }
    }

    /**
     * 记录车辆零件信息变更日志
     *
     * @param vehiclePartPo 车辆零件对象
     * @param remark        变更备注
     */
    private void recordLog(VehiclePartPo vehiclePartPo, String remark) {
        VehiclePartHistoryPo history = VehiclePartPoAssembler.INSTANCE.toHistory(vehiclePartPo);
        history.setDescription(remark);
        vehiclePartHistoryDao.insertPo(history);
    }

}
