package net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache;

import net.hwyz.iov.cloud.iov.ota.service.domain.activity.model.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.model.TaskDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.VehicleDo;

import java.util.List;
import java.util.Optional;

/**
 * 缓存服务接口
 *
 * @author hwyz_leo
 */
public interface CacheService {

    /**
     * 获取车辆领域对象缓存
     *
     * @param vin 车架号
     * @return 车辆领域对象
     */
    Optional<VehicleDo> getVehicle(String vin);

    /**
     * 设置车辆领域对象缓存
     *
     * @param vehicle 车辆领域对象
     */
    void setVehicle(VehicleDo vehicle);

    /**
     * 获取升级活动缓存
     *
     * @param activityId 升级活动ID
     * @return 升级活动
     */
    Optional<ActivityDo> getActivity(Long activityId);

    /**
     * 设置升级活动缓存
     *
     * @param activity 升级活动
     */
    void setActivity(ActivityDo activity);

    /**
     * 添加已发布升级活动缓存
     *
     * @param activity 升级活动
     */
    void addReleaseActivity(ActivityDo activity);

    /**
     * 删除已发布升级活动缓存
     *
     * @param activity 升级活动
     */
    void removeReleaseActivity(ActivityDo activity);

    /**
     * 获取已发布升级活动缓存
     *
     * @return 升级活动ID列表
     */
    List<Long> getReleaseActivity();

    /**
     * 获取升级任务缓存
     *
     * @param taskId 升级任务ID
     * @return 升级任务
     */
    Optional<TaskDo> getTask(Long taskId);

    /**
     * 设置升级任务缓存
     *
     * @param task 升级任务
     */
    void setTask(TaskDo task);

    /**
     * 添加已发布升级任务缓存
     *
     * @param task 升级任务
     */
    void addReleaseTask(TaskDo task);

    /**
     * 删除已发布升级任务缓存
     *
     * @param task 升级任务
     */
    void removeReleaseTask(TaskDo task);

    /**
     * 获取升级活动下已发布升级任务缓存
     *
     * @param activityId 升级活动ID
     * @return 升级任务ID列表
     */
    List<Long> getActivityReleaseTask(Long activityId);

    /**
     * 获取车辆升级任务缓存
     *
     * @param vin 车架号
     * @return 升级任务ID
     */
    Optional<Long> getVehicleTask(String vin);

}
