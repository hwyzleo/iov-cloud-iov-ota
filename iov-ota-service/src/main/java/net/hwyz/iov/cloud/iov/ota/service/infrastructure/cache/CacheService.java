package net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;

import java.util.List;
import java.util.Optional;

/**
 * 缓存服务接口
 *
 * @author hwyz_leo
 */
public interface CacheService {

    Optional<VehicleDo> getVehicle(String vin);

    void setVehicle(VehicleDo vehicle);

    Optional<ActivityDo> getActivity(Long activityId);

    void setActivity(ActivityDo activity);

    /**
     * 移除升级活动内存缓存
     * 供绕过仓库直接写库的链路（审批/型批评估/编辑）失效过期领域对象，
     * 避免后续经 getById 读到过期状态
     *
     * @param activityId 升级活动ID
     */
    void removeActivity(Long activityId);

    void addReleaseActivity(ActivityDo activity);

    void removeReleaseActivity(ActivityDo activity);

    List<Long> getReleaseActivity();

    Optional<Task> getTask(Long taskId);

    void setTask(Task task);

    void removeTask(Long taskId);

    void addReleaseTask(Task task);

    void removeReleaseTask(Task task);

    List<Long> getActivityReleaseTask(Long activityId);

    Optional<Long> getVehicleTask(String vin);

}
