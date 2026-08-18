package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.assembler.TaskAssembler;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OptimisticLockException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskInstallConditionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ApprovalDomainService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TargetResolutionDomainService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.event.publisher.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CR-015 P2-A 排程乐观锁测试（§5）
 * <p>schedule 更新条件必须包含当前 state 与 rowVersion；冲突返回明确乐观锁错误。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskAppService.scheduleTask 乐观锁")
class TaskAppServiceScheduleTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskInstallConditionRepository taskInstallConditionRepository;
    @Mock private TaskAssembler taskAssembler;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private ActivityAppService activityAppService;
    @Mock private ApprovalDomainService approvalDomainService;
    @Mock private TargetResolutionDomainService targetResolutionDomainService;
    @Mock private TaskReleaseGateService taskReleaseGateService;
    @Mock private TaskReportAppService taskReportAppService;

    @InjectMocks
    private TaskAppService service;

    private Task task;

    @BeforeEach
    void setUp() {
        task = Task.create(TaskId.of(1L), "任务A", TaskType.NORMAL, ActivityId.of(100L));
        task.setState(TaskState.APPROVED);
        when(taskRepository.getById(any())).thenReturn(Optional.of(task));
        when(taskAssembler.toResult(any())).thenReturn(null);
    }

    @Test
    @DisplayName("rowVersion 匹配 -> 排程成功")
    void schedule_rowVersionMatch_success() {
        when(taskRepository.scheduleWithOptimisticLock(any(), eq(5))).thenReturn(true);

        Instant releaseTime = Instant.now().plusSeconds(3600);
        service.scheduleTask(1L, releaseTime, 5);

        assertEquals(TaskState.SCHEDULED, task.getState());
        verify(taskRepository).scheduleWithOptimisticLock(task, 5);
        verify(taskRepository, never()).save(task);
        verify(eventPublisher).publishAll(any());
    }

    @Test
    @DisplayName("rowVersion 冲突 -> 抛 OptimisticLockException，未发布事件")
    void schedule_rowVersionConflict_throws() {
        when(taskRepository.scheduleWithOptimisticLock(any(), eq(5))).thenReturn(false);

        Instant releaseTime = Instant.now().plusSeconds(3600);
        assertThrows(OptimisticLockException.class, () -> service.scheduleTask(1L, releaseTime, 5));
        verify(eventPublisher, never()).publishAll(any());
    }

    @Test
    @DisplayName("未携带 rowVersion -> 回退取库内当前版本")
    void schedule_noRowVersion_fallsBackToDb() {
        when(taskRepository.getRowVersion(any())).thenReturn(3);
        when(taskRepository.scheduleWithOptimisticLock(any(), eq(3))).thenReturn(true);

        service.scheduleTask(1L, Instant.now().plusSeconds(3600), null);

        verify(taskRepository).getRowVersion(TaskId.of(1L));
        verify(taskRepository).scheduleWithOptimisticLock(task, 3);
    }
}
