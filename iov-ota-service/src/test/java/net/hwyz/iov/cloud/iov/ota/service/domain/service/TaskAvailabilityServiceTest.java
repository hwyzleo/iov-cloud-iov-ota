package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.AvailabilityStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskAvailabilityService 时间守卫测试（CR-012 §5.2）
 *
 * @author hwyz_leo
 */
@DisplayName("TaskAvailabilityService 时间守卫")
class TaskAvailabilityServiceTest {

    private TaskAvailabilityService service;

    private final Instant now = Instant.now();
    private final Instant releaseAt = now.minusSeconds(60);
    private final Instant startTime = now.plusSeconds(60);
    private final Instant endTime = now.plusSeconds(3600);

    @BeforeEach
    void setUp() {
        service = new TaskAvailabilityService();
    }

    @Test
    @DisplayName("RELEASED 且 releaseAt<=now<startTime：可见可预下载，不可申请安装")
    void released_beforeStartTime_visibleButNotInstallable() {
        Task task = buildReleasedTask(releaseAt, startTime, endTime);
        VehicleTask vt = buildVehicleTask(VehicleTaskStatus.READY_TO_INSTALL);

        TaskAvailabilityService.AvailabilityResult result = service.evaluate(task, vt, now);

        assertEquals(AvailabilityStatus.AVAILABLE, result.getAvailabilityStatus());
        assertTrue(result.isVisible());
        assertTrue(result.isDownloadAllowed());
        assertFalse(result.isInstallRequestAllowed());
    }

    @Test
    @DisplayName("IN_PROGRESS 且 startTime<=now<endTime：可申请安装")
    void inProgress_withinWindow_installable() {
        Instant midStart = now.minusSeconds(30);
        Task task = buildReleasedTask(releaseAt, midStart, endTime);
        task.activateRollout();
        assertEquals(TaskState.IN_PROGRESS, task.getState());
        VehicleTask vt = buildVehicleTask(VehicleTaskStatus.READY_TO_INSTALL);

        TaskAvailabilityService.AvailabilityResult result = service.evaluate(task, vt, now);

        assertTrue(result.isVisible());
        assertTrue(result.isInstallRequestAllowed());
    }

    @Test
    @DisplayName("now>=endTime：不可申请安装")
    void afterEndTime_notInstallable() {
        Instant pastEnd = now.minusSeconds(1);
        Task task = buildReleasedTask(releaseAt, startTime, pastEnd);
        VehicleTask vt = buildVehicleTask(VehicleTaskStatus.READY_TO_INSTALL);

        TaskAvailabilityService.AvailabilityResult result = service.evaluate(task, vt, now);

        assertTrue(result.isVisible());
        assertFalse(result.isInstallRequestAllowed());
    }

    @Test
    @DisplayName("PAUSED 任务：availabilityStatus=PAUSED，不可下载不可安装")
    void pausedTask_blocked() {
        Task task = buildReleasedTask(releaseAt, startTime, endTime);
        task.pause();
        VehicleTask vt = buildVehicleTask(VehicleTaskStatus.READY_TO_INSTALL);

        TaskAvailabilityService.AvailabilityResult result = service.evaluate(task, vt, now);

        assertEquals(AvailabilityStatus.PAUSED, result.getAvailabilityStatus());
        assertFalse(result.isDownloadAllowed());
        assertFalse(result.isInstallRequestAllowed());
    }

    @Test
    @DisplayName("CANCELED 任务：availabilityStatus=CANCELED")
    void canceledTask_canceled() {
        Task task = buildReleasedTask(releaseAt, startTime, endTime);
        task.cancel();
        VehicleTask vt = buildVehicleTask(VehicleTaskStatus.READY_TO_INSTALL);

        TaskAvailabilityService.AvailabilityResult result = service.evaluate(task, vt, now);

        assertEquals(AvailabilityStatus.CANCELED, result.getAvailabilityStatus());
    }

    @Test
    @DisplayName("VehicleTask 不在就绪状态族：不可申请安装")
    void vehicleTaskNotReady_notInstallable() {
        Task task = buildReleasedTask(releaseAt, now.minusSeconds(30), endTime);
        VehicleTask vt = buildVehicleTask(VehicleTaskStatus.EXECUTING);

        TaskAvailabilityService.AvailabilityResult result = service.evaluate(task, vt, now);

        assertTrue(result.isVisible());
        assertFalse(result.isInstallRequestAllowed());
    }

    @Test
    @DisplayName("未发布任务：availabilityStatus=NOT_RELEASED，不可见")
    void notReleased_notVisible() {
        Task task = Task.create(TaskId.of(1L), "t", TaskType.NORMAL, ActivityId.of(100L));
        VehicleTask vt = buildVehicleTask(VehicleTaskStatus.READY_TO_INSTALL);

        TaskAvailabilityService.AvailabilityResult result = service.evaluate(task, vt, now);

        assertEquals(AvailabilityStatus.NOT_RELEASED, result.getAvailabilityStatus());
        assertFalse(result.isVisible());
    }

    @Test
    @DisplayName("releaseAt 前不可见")
    void beforeReleaseAt_notVisible() {
        Task task = buildReleasedTask(now.plusSeconds(60), startTime, endTime);
        VehicleTask vt = buildVehicleTask(VehicleTaskStatus.READY_TO_INSTALL);

        TaskAvailabilityService.AvailabilityResult result = service.evaluate(task, vt, now);

        assertEquals(AvailabilityStatus.NOT_RELEASED, result.getAvailabilityStatus());
        assertFalse(result.isVisible());
    }

    private Task buildReleasedTask(Instant releaseAt, Instant startTime, Instant endTime) {
        Task task = Task.create(TaskId.of(1L), "测试任务", TaskType.NORMAL, ActivityId.of(100L));
        task.submit();
        task.approve(true, null);
        task.setStartTime(startTime);
        // 发布校验要求 endTime 在未来，先设未来值发布，再覆盖为测试值
        task.setEndTime(now.plusSeconds(7200));
        task.release(Set.of(Vin.of("VIN001")), "IMMEDIATE");
        task.setReleaseTime(releaseAt);
        task.setEndTime(endTime);
        return task;
    }

    private VehicleTask buildVehicleTask(VehicleTaskStatus status) {
        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(1L), 1L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                releaseAt, startTime, endTime);
        // 通过状态机推进到目标状态
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.grantConsent(false); // READY_TO_INSTALL
        if (status == VehicleTaskStatus.EXECUTING) {
            vt.attachExecution(
                    net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId.of(1L), 1);
        }
        return vt;
    }
}
