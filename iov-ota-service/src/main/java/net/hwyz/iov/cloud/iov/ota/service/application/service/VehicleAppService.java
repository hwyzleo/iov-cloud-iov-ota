package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleManagementResult;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehStatusMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleProjectionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 升级车辆应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleAppService {

    private final VehStatusMapper vehStatusDao;
    private final VehicleProjectionMapper vehicleProjectionMapper;

    /**
     * 查询车辆信息（主档投影 LEFT JOIN 清单上报运行态，CR-022 §5.1）
     *
     * @param vin       车架号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆管理查询结果列表（含 lastInventoryReportedAt）
     */
    public List<VehicleManagementResult> search(String vin, Date beginTime, Date endTime) {
        return vehicleProjectionMapper.selectManagementList(vin, beginTime, endTime);
    }

    /**
     * 根据车架号获取车辆管理详情（主档投影 LEFT JOIN 清单上报运行态，CR-022 §5.1）
     *
     * @param vin 车架号
     * @return 车辆管理查询结果，主档不存在返回 null
     */
    public VehicleManagementResult getVehicleByVin(String vin) {
        return vehicleProjectionMapper.selectManagementByVin(vin);
    }

    /**
     * 查询车辆信息（从车辆状态表，保留兼容）
     *
     * @param vin       车架号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆状态列表
     */
    public List<VehStatusPo> searchVehStatus(String vin, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return vehStatusDao.selectPoByMap(map);
    }

    /**
     * 根据车架号获取车辆状态信息（保留兼容）
     *
     * @param vin 车架号
     * @return 车辆状态
     */
    public VehStatusPo getVehStatusByVin(String vin) {
        return vehStatusDao.selectByVin(vin);
    }

}
