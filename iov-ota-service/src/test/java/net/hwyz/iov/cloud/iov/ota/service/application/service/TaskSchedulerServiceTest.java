package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskSchedulerService 到点自动发布")
class TaskSchedulerServiceTest {

    @Mock private TaskAppService taskAppService;
    @Mock private TaskRepository taskRepository;

    @InjectMocks
    private TaskSchedulerService scheduler;

    private Task scheduledTask(Instant releaseTime, Instant endTime) {
        Task task = Task.create(TaskId.of(1L), "测试任务", TaskType.NORMAL, ActivityId.of(10L));
        task.setUpgradeMode(UpgradeMode.NORMAL);
        task.setState(TaskState.SCHEDULED);
        task.setReleaseTime(releaseTime);
        task.setEndTime(endTime);
        return task;
    }

    @Test
    @DisplayName("已超过结束时间的排程任务 -> 跳过，不尝试发布也不落库")
    void pastEndTime_skipsRelease() {
        Instant now = Instant.now();
        Task task = scheduledTask(now.minusSeconds(60), now.minusSeconds(30));
        when(taskRepository.findScheduledTasks()).thenReturn(List.of(task));

        scheduler.autoReleaseScheduledTasks();

        verify(taskAppService, never()).releaseTaskByScheduler(anyLong());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("持续相同失败 -> 错误摘要只落库一次，不重复写入")
    void persistentFailure_savesErrorOnce() {
        Instant now = Instant.now();
        Task task = scheduledTask(now.minusSeconds(60), now.plusSeconds(3600));
        when(taskRepository.findScheduledTasks()).thenReturn(List.of(task));
        when(taskRepository.getById(TaskId.of(1L))).thenReturn(Optional.of(task));
        when(taskAppService.releaseTaskByScheduler(1L)).thenThrow(new IllegalStateException("定时发布失败"));

        scheduler.autoReleaseScheduledTasks();
        scheduler.autoReleaseScheduledTasks();

        // 两次都会尝试发布，但相同错误摘要只写一次
        verify(taskAppService, times(2)).releaseTaskByScheduler(1L);
        verify(taskRepository, times(1)).save(task);
    }
}
