package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehStatusMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleProjectionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
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
     * 查询车辆信息（从车辆投影表）
     *
     * @param vin       车架号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆投影列表
     */
    public List<VehicleProjectionPo> search(String vin, Date beginTime, Date endTime) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehicleProjectionPo> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (vin != null && !vin.isEmpty()) {
            wrapper.like("vin", vin);
        }
        if (beginTime != null) {
            wrapper.ge("create_time", beginTime);
        }
        if (endTime != null) {
            wrapper.le("create_time", endTime);
        }
        wrapper.orderByDesc("id");
        return vehicleProjectionMapper.selectList(wrapper);
    }

    /**
     * 根据车架号获取车辆投影信息
     *
     * @param vin 车架号
     * @return 车辆投影
     */
    public VehicleProjectionPo getVehicleByVin(String vin) {
        return vehicleProjectionMapper.selectByVin(vin);
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
