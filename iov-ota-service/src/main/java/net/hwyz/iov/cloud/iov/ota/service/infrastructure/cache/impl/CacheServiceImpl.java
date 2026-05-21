package net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskVehicleState;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_KEY_RELEASE_ACTIVITY = "fota:release-activity:";
    private static final String REDIS_KEY_PREFIX_ACTIVITY = "fota:activity:";
    private static final String REDIS_KEY_PREFIX_TASK = "fota:task:";

    private static final ConcurrentHashMap<Long, ActivityDo> activityMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Task> taskMap = new ConcurrentHashMap<>();

    @Override
    public Optional<VehicleDo> getVehicle(String vin) {
        return Optional.empty();
    }

    @Override
    public void setVehicle(VehicleDo vehicle) {
    }

    @Override
    public Optional<ActivityDo> getActivity(Long activityId) {
        return Optional.ofNullable(activityMap.get(activityId));
    }

    @Override
    public void setActivity(ActivityDo activity) {
        activityMap.put(activity.getId(), activity);
    }

    @Override
    public void addReleaseActivity(ActivityDo activity) {
        redisTemplate.opsForZSet().add(REDIS_KEY_RELEASE_ACTIVITY, activity.getId().toString(), activity.getStartTime().getTime());
    }

    @Override
    public void removeReleaseActivity(ActivityDo activity) {
        redisTemplate.opsForZSet().remove(REDIS_KEY_RELEASE_ACTIVITY, activity.getId().toString());
        redisTemplate.delete(REDIS_KEY_PREFIX_ACTIVITY + activity.getId());
    }

    @Override
    public List<Long> getReleaseActivity() {
        Set<String> releaseActivityIdList = redisTemplate.opsForZSet().range(REDIS_KEY_RELEASE_ACTIVITY, 0, -1);
        if (releaseActivityIdList == null) {
            return new ArrayList<>();
        }
        return releaseActivityIdList.stream().map(Long::parseLong).collect(Collectors.toList());
    }

    @Override
    public Optional<Task> getTask(Long taskId) {
        return Optional.ofNullable(taskMap.get(taskId));
    }

    @Override
    public void setTask(Task task) {
        taskMap.put(task.getId().getValue(), task);
    }

    @Override
    public void removeTask(Long taskId) {
        taskMap.remove(taskId);
    }

    @Override
    public void addReleaseTask(Task task) {
        redisTemplate.opsForZSet().add(REDIS_KEY_PREFIX_ACTIVITY + task.getActivityId().getValue(), task.getId().getValue().toString(), task.getStartTime().toEpochMilli());
        Map<String, Object> taskVehicleStateMap = new HashMap<>();
        if (task.getVehicles() != null) {
            for (Vin vin : task.getVehicles()) {
                taskVehicleStateMap.put(vin.getValue(), String.valueOf(TaskVehicleState.WAITING_DOWNLOAD.value));
            }
        }
        redisTemplate.opsForHash().putAll(REDIS_KEY_PREFIX_TASK + task.getId().getValue(), taskVehicleStateMap);
        redisTemplate.expireAt(REDIS_KEY_PREFIX_TASK + task.getId().getValue(), Date.from(task.getEndTime()));
    }

    @Override
    public void removeReleaseTask(Task task) {
        redisTemplate.opsForZSet().remove(REDIS_KEY_PREFIX_ACTIVITY + task.getActivityId().getValue(), task.getId().getValue().toString());
        redisTemplate.delete(REDIS_KEY_PREFIX_TASK + task.getId().getValue());
    }

    @Override
    public List<Long> getActivityReleaseTask(Long activityId) {
        Set<String> taskIdList = redisTemplate.opsForZSet().range(REDIS_KEY_PREFIX_ACTIVITY + activityId, 0, -1);
        if (taskIdList == null) {
            return new ArrayList<>();
        }
        return taskIdList.stream().map(Long::parseLong).collect(Collectors.toList());
    }

    @Override
    public Optional<Long> getVehicleTask(String vin) {
        long task = 0;
        long taskStartTime = 0;
        for (Long activityId : getReleaseActivity()) {
            for (Long taskId : getActivityReleaseTask(activityId)) {
                Object taskVehicleState = redisTemplate.opsForHash().get(REDIS_KEY_PREFIX_TASK + taskId, vin);
                if (taskVehicleState != null && Integer.parseInt(taskVehicleState.toString()) != TaskVehicleState.UPGRADE_SUCCESS.value) {
                    Double time = redisTemplate.opsForZSet().score(REDIS_KEY_PREFIX_ACTIVITY + activityId, taskId.toString());
                    if (time != null && (taskStartTime == 0 || time < taskStartTime)) {
                        task = taskId;
                        taskStartTime = time.longValue();
                    }
                }
            }
        }
        return Optional.ofNullable(task > 0 ? task : null);
    }
}