package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.assembler.TaskAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.TaskCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.TaskOrderException;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IOV-OTA-DSN-CR-017 单元测试：Task 创建应用服务服务端并发排号与 previousTaskId 自动推导
 * <p>覆盖：空作用域首波、同 phase 连续排号与前序链、Activity 行锁调用、显式序号冲突、\n
 * 显式前序校验、自动推导缺失/歧义 fail-closed、删除守卫。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskAppService.createTask 服务端排号与前序推导")
class TaskAppServiceCreateTest {

    /** 带内存 map 行为的 TaskRepository mock：可 verify，也可按场景覆写 */
    private TaskRepository taskRepository;
    private final Map<Long, Task> store = new ConcurrentHashMap<>();
    private final AtomicInteger lockCalls = new AtomicInteger();

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
        lockCalls.set(0);
        taskRepository = buildMockRepository();

        taskAppService = new TaskAppService(
                taskRepository, taskInstallConditionRepository, new TaskAssembler(), eventPublisher,
                activityAppService, approvalDomainService, targetResolutionDomainService,
                taskReleaseGateService, taskReportAppService, taskReportRepository, taskReleaseGateRepository);
        lenient().when(activityAppService.getActivityById(anyLong())).thenReturn(null);
        lenient().when(taskReportRepository.findLatestByTaskId(anyLong())).thenReturn(Optional.empty());
        lenient().when(taskReleaseGateRepository.getByNextTaskId(anyLong())).thenReturn(Optional.empty());
    }

    private TaskRepository buildMockRepository() {
        TaskRepository repo = mock(TaskRepository.class);
        lenient().when(repo.getById(any())).thenAnswer(inv -> Optional.ofNullable(store.get(((TaskId) inv.getArgument(0)).getValue())));
        lenient().doAnswer(inv -> {
            Task t = inv.getArgument(0);
            store.put(t.getId().getValue(), t);
            return null;
        }).when(repo).save(any());
        lenient().doAnswer(inv -> {
            lockCalls.incrementAndGet();
            return null;
        }).when(repo).lockActivity(anyLong());
        lenient().when(repo.findMaxSequence(anyLong(), any())).thenAnswer(inv ->
                maxSequence(inv.getArgument(0), inv.getArgument(1)));
        lenient().when(repo.findByActivityPhaseSequence(anyLong(), any(), anyLong())).thenAnswer(inv ->
                findBySequence(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
        lenient().when(repo.isReferencedAsPrevious(anyLong())).thenAnswer(inv ->
                store.values().stream().anyMatch(t -> inv.getArgument(0).equals(t.getPreviousTaskId())));
        return repo;
    }

    private Long maxSequence(Long activityId, TaskPhase phase) {
        return store.values().stream()
                .filter(t -> t.getActivityId().getValue().equals(activityId) && t.getPhase() == phase)
                .map(Task::getSequenceNo)
                .filter(java.util.Objects::nonNull)
                .map(Integer::longValue)
                .max(Long::compareTo)
                .orElse(null);
    }

    private List<Task> findBySequence(Long activityId, TaskPhase phase, Long sequenceNo) {
        return store.values().stream()
                .filter(t -> t.getActivityId().getValue().equals(activityId)
                        && t.getPhase() == phase
                        && t.getSequenceNo() != null
                        && t.getSequenceNo().longValue() == sequenceNo)
                .collect(Collectors.toList());
    }

    private TaskCreateCmd buildCmd(Long activityId) {
        return TaskCreateCmd.builder()
                .name("波次任务")
                .activityId(activityId)
                .target("{\"mode\":\"LIST\",\"vins\":[\"VIN001\"]}")
                .startTime(Instant.parse("2026-09-01T00:00:00Z"))
                .endTime(Instant.parse("2026-09-30T00:00:00Z"))
                .upgradeMode("1")
                .build();
    }

    private Task seedTask(long id, long activityId, int seq, Long prev, TaskPhase phase) {
        Task task = Task.create(TaskId.of(id), "种子" + id, TaskType.NORMAL, ActivityId.of(activityId));
        task.setSequenceNo(seq);
        task.setPreviousTaskId(prev);
        task.setPhase(phase);
        store.put(id, task);
        return task;
    }

    @Test
    @DisplayName("空作用域创建 -> sequenceNo=0、previousTaskId=NULL，且先锁定 Activity")
    void emptyScope_firstWave() {
        TaskResult result = taskAppService.createTask(buildCmd(100L));
        assertEquals(0, result.getSequenceNo());
        assertNull(result.getPreviousTaskId());
        verify(taskRepository).lockActivity(100L);
        assertEquals(1, lockCalls.get());
    }

    @Test
    @DisplayName("同 phase 连续创建 -> 0→1→2，前序链正确")
    void sequentialWaves_chain() {
        TaskResult r0 = taskAppService.createTask(buildCmd(100L));
        TaskResult r1 = taskAppService.createTask(buildCmd(100L));
        TaskResult r2 = taskAppService.createTask(buildCmd(100L));

        assertEquals(0, r0.getSequenceNo());
        assertNull(r0.getPreviousTaskId());

        assertEquals(1, r1.getSequenceNo());
        assertEquals(r0.getTaskId(), r1.getPreviousTaskId());

        assertEquals(2, r2.getSequenceNo());
        assertEquals(r1.getTaskId(), r2.getPreviousTaskId());
    }

    @Test
    @DisplayName("不同 Activity 各自独立排号，互不影响")
    void differentActivity_independentScope() {
        TaskResult a0 = taskAppService.createTask(buildCmd(100L));
        TaskResult b0 = taskAppService.createTask(buildCmd(200L));

        assertEquals(0, a0.getSequenceNo());
        assertEquals(0, b0.getSequenceNo());
        assertNull(a0.getPreviousTaskId());
        assertNull(b0.getPreviousTaskId());
    }

    @Test
    @DisplayName("显式序号等于服务端下一序号 -> 通过")
    void explicitSequence_matching_ok() {
        taskAppService.createTask(buildCmd(100L));
        TaskCreateCmd cmd = buildCmd(100L);
        cmd.setSequenceNo(1);
        TaskResult result = taskAppService.createTask(cmd);
        assertEquals(1, result.getSequenceNo());
    }

    @Test
    @DisplayName("显式序号跳号/复用旧序号 -> 拒绝 SEQUENCE_CONFLICT")
    void explicitSequence_conflict_rejected() {
        taskAppService.createTask(buildCmd(100L));
        TaskCreateCmd skip = buildCmd(100L);
        skip.setSequenceNo(5);
        TaskOrderException ex = assertThrows(TaskOrderException.class, () -> taskAppService.createTask(skip));
        assertEquals(TaskOrderException.ERROR_CODE_SEQUENCE_CONFLICT, ex.getCode());
    }

    @Test
    @DisplayName("显式前序合法（同 Activity、序号小）-> 覆盖自动候选并持久化")
    void explicitPrevious_valid_ok() {
        TaskResult r0 = taskAppService.createTask(buildCmd(100L));
        TaskCreateCmd cmd = buildCmd(100L);
        cmd.setPreviousTaskId(r0.getTaskId());
        TaskResult r1 = taskAppService.createTask(cmd);
        assertEquals(r0.getTaskId(), r1.getPreviousTaskId());
        assertEquals(1, r1.getSequenceNo());
    }

    @Test
    @DisplayName("显式前序属于其他 Activity -> 拒绝 SCOPE_MISMATCH")
    void explicitPrevious_otherActivity_rejected() {
        TaskResult r0 = taskAppService.createTask(buildCmd(100L));
        TaskCreateCmd cmd = buildCmd(999L);
        cmd.setPreviousTaskId(r0.getTaskId());
        TaskOrderException ex = assertThrows(TaskOrderException.class, () -> taskAppService.createTask(cmd));
        assertEquals(TaskOrderException.ERROR_CODE_PREVIOUS_SCOPE_MISMATCH, ex.getCode());
    }

    @Test
    @DisplayName("显式前序不存在 -> 拒绝 PREVIOUS_NOT_FOUND")
    void explicitPrevious_notFound_rejected() {
        TaskCreateCmd cmd = buildCmd(100L);
        cmd.setPreviousTaskId(999999L);
        TaskOrderException ex = assertThrows(TaskOrderException.class, () -> taskAppService.createTask(cmd));
        assertEquals(TaskOrderException.ERROR_CODE_PREVIOUS_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("自动推导前序缺失（seq>0 且 sequence-1 无候选）-> fail-closed 拒绝")
    void autoPrevious_missing_failClosed() {
        seedTask(1L, 100L, 5, 4L, TaskPhase.VALIDATION);
        // 覆写候选查询返回空，模拟脏数据缺口：max=5 但 sequence-1=5 无有效候选
        when(taskRepository.findByActivityPhaseSequence(100L, TaskPhase.VALIDATION, 5L)).thenReturn(List.of());

        TaskCreateCmd cmd = buildCmd(100L);
        TaskOrderException ex = assertThrows(TaskOrderException.class, () -> taskAppService.createTask(cmd));
        assertEquals(TaskOrderException.ERROR_CODE_PREVIOUS_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("历史重复 sequence 产生多个候选 -> 拒绝 PREVIOUS_AMBIGUOUS")
    void autoPrevious_ambiguous_failClosed() {
        seedTask(101L, 100L, 0, null, TaskPhase.VALIDATION);
        seedTask(102L, 100L, 0, null, TaskPhase.VALIDATION);

        TaskCreateCmd cmd = buildCmd(100L);
        TaskOrderException ex = assertThrows(TaskOrderException.class, () -> taskAppService.createTask(cmd));
        assertEquals(TaskOrderException.ERROR_CODE_PREVIOUS_AMBIGUOUS, ex.getCode());
    }

    @Test
    @DisplayName("删除被后续任务引用为前序的任务 -> 拒绝删除（只可取消）")
    void deleteReferencedTask_guard() {
        TaskResult r0 = taskAppService.createTask(buildCmd(100L));
        TaskResult r1 = taskAppService.createTask(buildCmd(100L));
        assertEquals(r0.getTaskId(), r1.getPreviousTaskId());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> taskAppService.deleteTaskByIds(new Long[]{r0.getTaskId()}));
        assertTrue(ex.getMessage().contains("只能取消"));
    }
}
