package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 发布时生成 VehicleTask 快照的回归测试。
 *
 * <p>缺陷：发布任务只写入 activityId/taskId/vin，vehicle_task_status 为 NULL，
 * 而 VehicleTaskMapper.selectByVin/selectByTaskId 均以 vehicle_task_status IS NOT NULL
 * 过滤，导致车辆任务对车端检测与管理端查询不可见。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskVehicleRepositoryImpl 发布快照初始化")
class TaskVehicleRepositoryImplTest {

    @Mock
    private TaskVehicleMapper taskVehicleDao;

    @InjectMocks
    private TaskVehicleRepositoryImpl repository;

    private Task releasedTask(Instant releaseAt, Instant startTime, Instant endTime) {
        Task task = Task.create(TaskId.of(9001L), "验证任务", TaskType.NORMAL, ActivityId.of(100L));
        task.setPhase(TaskPhase.VALIDATION);
        task.setState(TaskState.RELEASED);
        task.setReleaseTime(releaseAt);
        task.setActualReleaseTime(releaseAt);
        task.setStartTime(startTime);
        task.setEndTime(endTime);
        return task;
    }

    @Test
    @DisplayName("发布写入 vehicle_task_status / release_at / 执行窗口 / 修订号，车辆任务不再因状态为空被过滤")
    void batchCreate_initializesVehicleTaskSnapshot() {
        Instant releaseAt = Instant.parse("2026-11-09T02:00:00Z");
        Instant startTime = Instant.parse("2026-11-09T01:00:00Z");
        Instant endTime = Instant.parse("2026-11-10T01:00:00Z");
        Task task = releasedTask(releaseAt, startTime, endTime);

        when(taskVehicleDao.selectByTaskIdAndVin(anyLong(), anyString())).thenReturn(null);
        when(taskVehicleDao.batchInsertPo(anyList())).thenReturn(2);

        int created = repository.batchCreate(task, Set.of(Vin.of("VIN001"), Vin.of("VIN002")));
        assertEquals(2, created);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskVehiclePo>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskVehicleDao).batchInsertPo(captor.capture());
        List<TaskVehiclePo> records = captor.getValue();
        assertEquals(2, records.size());

        for (TaskVehiclePo po : records) {
            assertEquals(100L, po.getActivityId());
            assertEquals(9001L, po.getTaskId());
            assertNotNull(po.getVin());
            // 核心回归点：状态非空，否则读取侧 vehicle_task_status IS NOT NULL 会永久丢行
            assertEquals(VehicleTaskStatus.DOWNLOAD_PENDING.getValue(), po.getVehicleTaskStatus());
            assertEquals(1L, po.getTaskRevision());
            assertEquals(DownloadReadyState.NOT_STARTED.getValue(), po.getDownloadReadyState());
            assertEquals(0, po.getConsentRequired());
            assertEquals(ConsentState.NOT_REQUIRED.getValue(), po.getConsentState());
            // 发布时刻与执行窗口快照
            assertEquals(releaseAt, po.getReleaseAt().toInstant());
            assertEquals(startTime, po.getVtStartTime().toInstant());
            assertEquals(endTime, po.getVtEndTime().toInstant());
            assertEquals(0, po.getLastAttemptNo());
        }
    }

    @Test
    @DisplayName("重复发布幂等：已存在的 taskId+vin 不重复插入")
    void batchCreate_idempotentWhenVehicleTaskExists() {
        Task task = releasedTask(Instant.parse("2026-11-09T02:00:00Z"),
                Instant.parse("2026-11-09T01:00:00Z"), Instant.parse("2026-11-10T01:00:00Z"));
        when(taskVehicleDao.selectByTaskIdAndVin(anyLong(), anyString()))
                .thenReturn(TaskVehiclePo.builder().id(1L).taskId(9001L).vin("VIN001").build());

        int created = repository.batchCreate(task, Set.of(Vin.of("VIN001")));

        assertEquals(0, created);
        verify(taskVehicleDao, never()).batchInsertPo(anyList());
    }

    @Test
    @DisplayName("空目标车辆不产生任何写入")
    void batchCreate_noVehicles() {
        Task task = releasedTask(Instant.parse("2026-11-09T02:00:00Z"),
                Instant.parse("2026-11-09T01:00:00Z"), Instant.parse("2026-11-10T01:00:00Z"));

        assertEquals(0, repository.batchCreate(task, Set.of()));
        verify(taskVehicleDao, never()).batchInsertPo(anyList());
    }

    @Test
    @DisplayName("发布时刻优先取 actualReleaseTime，缺失时回落计划发布时间")
    void batchCreate_releaseAtFallback() {
        Instant planned = Instant.parse("2026-11-09T02:00:00Z");
        Task task = releasedTask(planned, Instant.parse("2026-11-09T01:00:00Z"),
                Instant.parse("2026-11-10T01:00:00Z"));
        task.setActualReleaseTime(null);

        when(taskVehicleDao.selectByTaskIdAndVin(anyLong(), anyString())).thenReturn(null);
        when(taskVehicleDao.batchInsertPo(anyList())).thenReturn(1);

        repository.batchCreate(task, Set.of(Vin.of("VIN001")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskVehiclePo>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskVehicleDao).batchInsertPo(captor.capture());
        TaskVehiclePo po = captor.getValue().get(0);
        assertNotNull(po.getReleaseAt());
        assertEquals(planned, po.getReleaseAt().toInstant());
    }
}
