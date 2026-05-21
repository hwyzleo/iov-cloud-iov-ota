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
