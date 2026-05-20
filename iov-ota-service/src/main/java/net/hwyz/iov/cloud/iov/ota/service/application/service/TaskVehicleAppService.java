package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.TaskVehicleDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.TaskVehicleProcessDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskVehiclePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskVehicleProcessPo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆升级任务应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskVehicleAppService {

    private final TaskVehicleDao taskVehicleDao;
    private final TaskVehicleProcessDao taskVehicleProcessDao;

    /**
     * 查询升级任务
     *
     * @param vin       车架号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 升级活动列表
     */
    public List<TaskVehiclePo> search(String vin, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return taskVehicleDao.selectPoByMap(map);
    }

    /**
     * 根据主键ID获取车辆升级任务
     *
     * @param id 主键ID
     * @return 车辆升级任务
     */
    public TaskVehiclePo getTaskVehicleById(Long id) {
        return taskVehicleDao.selectPoById(id);
    }

    /**
     * 新增升级任务车辆升级过程
     *
     * @param taskVehicleProcess 升级任务车辆升级过程
     */
    public void addTaskVehicleProcess(TaskVehicleProcessPo taskVehicleProcess) {
        taskVehicleProcessDao.insertPo(taskVehicleProcess);
    }

}
