package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.AvailabilityStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 任务可见性与时间守卫领域服务（CR-012 §5.2）
 *
 * <p>时间守卫规则：
 * <pre>
 * visible = task active && releaseAt <= now
 * canDownload = visible && packageReady && networkPolicyPassed && consentPassedIfRequired
 * canRequestInstall = visible && startTime <= now < endTime
 *                     && vehicleTaskStatus in READY states
 *                     && task/control not paused/canceled/superseded
 * </pre>
 *
 * <p>现有 §4.15.5 "RELEASED 且 now < startTime 不可命中"已删除，改为"可见但不可申请安装"。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class TaskAvailabilityService {

    /**
     * 评估车辆任务的可见性、下载与安装许可。
     *
     * @param task        升级任务
     * @param vehicleTask 车辆任务
     * @param now         当前时间
     * @return 可用性判定结果
     */
    public AvailabilityResult evaluate(Task task, VehicleTask vehicleTask, Instant now) {
        AvailabilityStatus availabilityStatus = computeAvailabilityStatus(task, vehicleTask, now);
        boolean visible = isVisible(task, now);
        boolean downloadAllowed = visible && isDownloadAllowed(vehicleTask);
        boolean installRequestAllowed = visible && canRequestInstall(task, vehicleTask, now);

        return new AvailabilityResult(availabilityStatus, visible, downloadAllowed, installRequestAllowed);
    }

    /**
     * 任务是否对车辆可见（releaseAt <= now 且任务处于发布后状态）。
     */
    public boolean isVisible(Task task, Instant now) {
        if (!isTaskReleasedActive(task)) {
            return false;
        }
        return task.getReleaseTime() == null || !now.isBefore(task.getReleaseTime());
    }

    /**
     * 是否可申请安装（时间窗口 + 车辆任务就绪状态 + 任务未暂停/取消/取代）。
     */
    public boolean canRequestInstall(Task task, VehicleTask vehicleTask, Instant now) {
        if (!isVisible(task, now)) {
            return false;
        }
        // 时间窗口：startTime <= now < endTime
        if (task.getStartTime() != null && now.isBefore(task.getStartTime())) {
            return false;
        }
        if (task.getEndTime() != null && !now.isBefore(task.getEndTime())) {
            return false;
        }
        // 车辆任务须处于就绪状态族
        if (vehicleTask == null || !vehicleTask.isInReadyState()) {
            return false;
        }
        // 任务未暂停/取消/取代
        if (task.getState() == TaskState.PAUSED
                || task.getState() == TaskState.CANCELED
                || task.getState() == TaskState.SUPERSEDED) {
            return false;
        }
        return true;
    }

    private boolean isDownloadAllowed(VehicleTask vehicleTask) {
        if (vehicleTask == null) {
            return false;
        }
        // 包准备、网络策略、授权门禁满足时可预下载
        // 具体门禁由应用服务聚合，此处仅判定车辆任务非终态且授权通过
        if (vehicleTask.isTerminal()) {
            return false;
        }
        return true;
    }

    private AvailabilityStatus computeAvailabilityStatus(Task task, VehicleTask vehicleTask, Instant now) {
        if (task == null || vehicleTask == null) {
            return AvailabilityStatus.NONE;
        }
        TaskState taskState = task.getState();
        if (taskState == TaskState.PAUSED) {
            return AvailabilityStatus.PAUSED;
        }
        if (taskState == TaskState.CANCELED) {
            return AvailabilityStatus.CANCELED;
        }
        if (taskState == TaskState.SUPERSEDED) {
            return AvailabilityStatus.SUPERSEDED;
        }
        if (!isTaskReleasedActive(task)) {
            return AvailabilityStatus.NOT_RELEASED;
        }
        if (task.getReleaseTime() != null && now.isBefore(task.getReleaseTime())) {
            return AvailabilityStatus.NOT_RELEASED;
        }
        return AvailabilityStatus.AVAILABLE;
    }

    /**
     * 任务是否处于发布后可检测状态（RELEASED 或 IN_PROGRESS）。
     */
    private boolean isTaskReleasedActive(Task task) {
        return task.getState() == TaskState.RELEASED
                || task.getState() == TaskState.IN_PROGRESS;
    }

    /**
     * 可用性判定结果。
     */
    @Getter
    @RequiredArgsConstructor
    public static class AvailabilityResult {
        private final AvailabilityStatus availabilityStatus;
        private final boolean visible;
        private final boolean downloadAllowed;
        private final boolean installRequestAllowed;
    }
}
