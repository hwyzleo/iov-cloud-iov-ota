package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.assembler.TaskAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskInstallConditionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReleaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ApprovalDomainService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TargetResolutionDomainService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.event.publisher.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试：TaskAppService.search 后台任务列表查询（state 过滤 / 全量 / name 与时间过滤）
 * <p>修复点：原列表仅通过 findReleasedTasks 返回「已发布」任务，新建的草稿任务在后台列表不可见；
 * 现按 state 过滤，state 为空返回全部状态。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskAppService.search 后台任务列表查询")
class TaskAppServiceSearchTest {

    private TaskRepository taskRepository;
    private final Map<Long, Task> store = new ConcurrentHashMap<>();

    @Mock private TaskInstallConditionRepository taskInstallConditionRepository;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private ActivityAppService activityAppService;
    @Mock private ApprovalDomainService approvalDomainService;
    @Mock private TargetResolutionDomainService targetResolutionDomainService;
    @Mock private TaskReleaseGateService taskReleaseGateService;
    @Mock private TaskReportAppService taskReportAppService;
    @Mock private TaskReportRepository taskReportRepository;
    @Mock private TaskReleaseGateRepository taskReleaseGateRepository;

    private TaskAppService taskAppService;

    @BeforeEach
    void setUp() {
        store.clear();
        taskRepository = buildMockRepository();
        taskAppService = new TaskAppService(
                taskRepository, taskInstallConditionRepository, new TaskAssembler(), eventPublisher,
                activityAppService, approvalDomainService, targetResolutionDomainService,
                taskReleaseGateService, taskReportAppService, taskReportRepository, taskReleaseGateRepository);
        lenient().when(taskReportRepository.findLatestByTaskId(anyLong())).thenReturn(Optional.empty());
        lenient().when(taskReleaseGateRepository.getByNextTaskId(anyLong())).thenReturn(Optional.empty());
    }

    private TaskRepository buildMockRepository() {
        TaskRepository repo = mock(TaskRepository.class);
        lenient().when(repo.findByState(any())).thenAnswer(inv ->
                store.values().stream()
                        .filter(t -> inv.getArgument(0) == null || t.getState().value == (int) inv.getArgument(0))
                        .collect(Collectors.toList()));
        return repo;
    }

    private Task seedTask(long id, String name, TaskState state) {
        Task task = Task.create(TaskId.of(id), name, TaskType.NORMAL, ActivityId.of(100L));
        task.setPhase(TaskPhase.VALIDATION);
        task.setStartTime(Instant.parse("2026-09-01T00:00:00Z"));
        task.setEndTime(Instant.parse("2026-09-30T00:00:00Z"));
        task.setState(state);
        store.put(id, task);
        return task;
    }

    @Test
    @DisplayName("新建的草稿任务（未发布）在列表中可见")
    void draftTask_isVisibleInList() {
        seedTask(1L, "草稿任务", TaskState.DRAFT);
        seedTask(2L, "已发布任务", TaskState.RELEASED);

        List<TaskResult> results = taskAppService.search(null, null, null, null);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getTaskId().equals(1L) && r.getState().equals(TaskState.DRAFT.name())));
        assertTrue(results.stream().anyMatch(r -> r.getTaskId().equals(2L) && r.getState().equals(TaskState.RELEASED.name())));
    }

    @Test
    @DisplayName("按 state 过滤 -> 仅返回该状态任务")
    void filterByState() {
        seedTask(1L, "草稿任务", TaskState.DRAFT);
        seedTask(2L, "待审批任务", TaskState.PENDING_APPROVAL);
        seedTask(3L, "已发布任务", TaskState.RELEASED);

        List<TaskResult> results = taskAppService.search(null, TaskState.DRAFT.value, null, null);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getTaskId());
        assertEquals(TaskState.DRAFT.name(), results.get(0).getState());
    }

    @Test
    @DisplayName("按 state 过滤已发布 -> 仅返回已发布任务")
    void filterByReleasedState() {
        seedTask(1L, "草稿任务", TaskState.DRAFT);
        seedTask(2L, "已发布任务", TaskState.RELEASED);

        List<TaskResult> results = taskAppService.search(null, TaskState.RELEASED.value, null, null);

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getTaskId());
        assertEquals(TaskState.RELEASED.name(), results.get(0).getState());
    }

    @Test
    @DisplayName("按名称过滤")
    void filterByName() {
        seedTask(1L, "验证波次A", TaskState.DRAFT);
        seedTask(2L, "灰度波次B", TaskState.DRAFT);

        List<TaskResult> results = taskAppService.search("波次", null, null, null);

        assertEquals(2, results.size());

        List<TaskResult> filtered = taskAppService.search("灰度", null, null, null);
        assertEquals(1, filtered.size());
        assertEquals(2L, filtered.get(0).getTaskId());
    }

    @Test
    @DisplayName("按开始/结束时间过滤")
    void filterByTimeRange() {
        Task early = seedTask(1L, "早期任务", TaskState.DRAFT);
        early.setStartTime(Instant.parse("2026-08-01T00:00:00Z"));
        early.setEndTime(Instant.parse("2026-08-15T00:00:00Z"));
        seedTask(2L, "九月任务", TaskState.DRAFT);

        List<TaskResult> results = taskAppService.search(null, null,
                java.util.Date.from(Instant.parse("2026-09-01T00:00:00Z")), null);

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getTaskId());
    }

    @Test
    @DisplayName("state 为空且无匹配名称 -> 返回空列表")
    void noMatch_returnsEmpty() {
        seedTask(1L, "草稿任务", TaskState.DRAFT);

        List<TaskResult> results = taskAppService.search("不存在的任务", null, null, null);

        assertTrue(results.isEmpty());
    }
}
