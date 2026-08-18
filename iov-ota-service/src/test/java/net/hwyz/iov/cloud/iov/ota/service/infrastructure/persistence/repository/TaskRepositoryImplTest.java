package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskStrategyType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskStrategy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.assembler.TaskPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskRestrictionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStrategyPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskRepositoryImpl.save 子表脏检查")
class TaskRepositoryImplTest {

    @Mock private TaskMapper taskMapper;
    @Mock private TaskRestrictionMapper taskRestrictionMapper;
    @Mock private TaskStrategyMapper taskStrategyMapper;
    @Mock private TaskVehicleMapper taskVehicleMapper;
    @Mock private TaskInstallConditionMapper taskInstallConditionMapper;
    @Mock private TaskMetricMapper taskMetricMapper;
    @Mock private TaskReportMapper taskReportMapper;
    @Mock private UpgradeLogMapper upgradeLogMapper;
    @Mock private TaskStateLogMapper taskStateLogMapper;
    @Mock private TaskApprovalMapper taskApprovalMapper;
    @Mock private TaskVehicleDetailMapper taskVehicleDetailMapper;
    @Mock private TaskVehicleRetryLogMapper taskVehicleRetryLogMapper;
    @Spy private TaskPoAssembler taskPoAssembler = new TaskPoAssembler();
    @Mock private CacheService cacheService;

    @InjectMocks
    private TaskRepositoryImpl repository;

    private Task taskWithStrategies(TaskStrategy... strategies) {
        Task task = Task.create(TaskId.of(1L), "测试任务", TaskType.NORMAL, ActivityId.of(10L));
        task.setUpgradeMode(UpgradeMode.NORMAL);
        task.loadRestrictionsAndStrategies(List.of(), List.of(strategies));
        return task;
    }

    private void stubExisting(String... strategyTypeAndExpr) {
        when(taskMapper.selectPoById(1L)).thenReturn(TaskPo.builder().id(1L).build());
        when(taskRestrictionMapper.selectPoByTaskId(1L)).thenReturn(List.of());
        List<TaskStrategyPo> existing = new java.util.ArrayList<>();
        for (int i = 0; i < strategyTypeAndExpr.length; i += 2) {
            existing.add(TaskStrategyPo.builder()
                    .id((long) (i / 2 + 1))
                    .taskId(1L)
                    .strategyType(strategyTypeAndExpr[i])
                    .strategyExpression(strategyTypeAndExpr[i + 1])
                    .build());
        }
        when(taskStrategyMapper.selectPoByTaskId(1L)).thenReturn(existing);
    }

    @Nested
    @DisplayName("策略行脏检查")
    class StrategyDirtyCheck {

        @Test
        @DisplayName("策略内容未变化 -> 不触发任何 updatePo/insertPo")
        void unchangedStrategy_skipsWrite() {
            Task task = taskWithStrategies(TaskStrategy.builder()
                    .id(1L).taskId(TaskId.of(1L)).type(TaskStrategyType.FLASH_COUNT).strategy("2").build());
            stubExisting("FLASH_COUNT", "2");

            repository.save(task);

            verify(taskStrategyMapper, never()).updatePo(any());
            verify(taskStrategyMapper, never()).insertPo(any());
            // 仍会完成主表与缓存更新
            verify(taskMapper, times(1)).updatePo(any());
            verify(cacheService).setTask(task);
        }

        @Test
        @DisplayName("多条策略仅表达式变化的一行触发 updatePo")
        void changedStrategy_updatesOnlyChangedRow() {
            Task task = taskWithStrategies(
                    TaskStrategy.builder().id(1L).taskId(TaskId.of(1L)).type(TaskStrategyType.FLASH_COUNT).strategy("3").build(),
                    TaskStrategy.builder().id(2L).taskId(TaskId.of(1L)).type(TaskStrategyType.KEEP_IN_PARK).strategy("1").build());
            stubExisting("FLASH_COUNT", "2", "KEEP_IN_PARK", "1");

            repository.save(task);

            verify(taskStrategyMapper, times(1)).updatePo(argThat(po -> po.getId() == 1L));
            verify(taskStrategyMapper, never()).updatePo(argThat(po -> po.getId() == 2L));
        }

        @Test
        @DisplayName("新增策略(id=null) -> 走 insertPo")
        void newStrategy_inserts() {
            Task task = taskWithStrategies(TaskStrategy.builder()
                    .id(null).taskId(TaskId.of(1L)).type(TaskStrategyType.HV_SOC).strategy("50").build());
            stubExisting();

            repository.save(task);

            verify(taskStrategyMapper, times(1)).insertPo(any());
            verify(taskStrategyMapper, never()).updatePo(any());
        }
    }

    @Nested
    @DisplayName("限制条件行脏检查")
    class RestrictionDirtyCheck {

        @Test
        @DisplayName("限制条件未变化 -> 不触发 updatePo")
        void unchangedRestriction_skipsWrite() {
            Task task = Task.create(TaskId.of(1L), "测试任务", TaskType.NORMAL, ActivityId.of(10L));
            task.setUpgradeMode(UpgradeMode.NORMAL);
            task.loadRestrictionsAndStrategies(
                    List.of(net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskRestriction.builder()
                            .id(1L)
                            .taskId(TaskId.of(1L))
                            .type(net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskRestrictionType.COMPARISON_CRITERIA)
                            .expression("V4.0.0")
                            .build()),
                    List.of());

            when(taskMapper.selectPoById(1L)).thenReturn(TaskPo.builder().id(1L).build());
            when(taskStrategyMapper.selectPoByTaskId(1L)).thenReturn(List.of());
            when(taskRestrictionMapper.selectPoByTaskId(1L)).thenReturn(List.of(
                    TaskRestrictionPo.builder()
                            .id(1L).taskId(1L)
                            .restrictionType("COMPARISON_CRITERIA")
                            .restrictionExpression("V4.0.0")
                            .build()));

            repository.save(task);

            verify(taskRestrictionMapper, never()).updatePo(any());
            verify(taskRestrictionMapper, never()).insertPo(any());
        }
    }

    @Nested
    @DisplayName("scheduleWithOptimisticLock 排程乐观锁")
    class ScheduleOptimisticLock {

        @Test
        @DisplayName("更新条件含 state 与 rowVersion，冲突返回 false")
        void scheduleLock_passThroughParams() {
            Task task = Task.create(TaskId.of(1L), "测试任务", TaskType.NORMAL, ActivityId.of(10L));
            task.setState(TaskState.SCHEDULED);
            task.setReleaseTime(Instant.parse("2026-08-20T00:00:00Z"));

            when(taskMapper.updateScheduleWithVersion(
                    1L, TaskState.APPROVED.value, 5,
                    Date.from(Instant.parse("2026-08-20T00:00:00Z")),
                    TaskState.SCHEDULED.value)).thenReturn(1);

            boolean ok = repository.scheduleWithOptimisticLock(task, 5);

            assertTrue(ok);
            verify(taskMapper).updateScheduleWithVersion(
                    1L, TaskState.APPROVED.value, 5,
                    Date.from(Instant.parse("2026-08-20T00:00:00Z")),
                    TaskState.SCHEDULED.value);
        }

        @Test
        @DisplayName("影响行数为 0 -> 乐观锁冲突返回 false")
        void scheduleLock_conflict_returnsFalse() {
            Task task = Task.create(TaskId.of(1L), "测试任务", TaskType.NORMAL, ActivityId.of(10L));
            task.setState(TaskState.SCHEDULED);
            task.setReleaseTime(Instant.parse("2026-08-20T00:00:00Z"));

            when(taskMapper.updateScheduleWithVersion(any(), any(), any(), any(), any())).thenReturn(0);

            boolean ok = repository.scheduleWithOptimisticLock(task, 5);

            assertFalse(ok);
            verify(cacheService, never()).setTask(any());
        }

        @Test
        @DisplayName("getRowVersion 从库内取当前版本")
        void getRowVersion_readsCurrent() {
            when(taskMapper.selectPoById(1L)).thenReturn(TaskPo.builder().id(1L).rowVersion(7).build());

            Integer version = repository.getRowVersion(TaskId.of(1L));

            assertEquals(7, version);
        }
    }
}
