package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.VehicleTaskSnapshotSeal;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskResetSeed;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskSnapshotMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskSnapshotPo;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private VehicleTaskSnapshotMapper vehicleTaskSnapshotDao;

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

        int created = repository.batchCreate(task, Set.of(Vin.of("VIN001"), Vin.of("VIN002")), null);
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

        int created = repository.batchCreate(task, Set.of(Vin.of("VIN001")), null);

        assertEquals(0, created);
        verify(taskVehicleDao, never()).batchInsertPo(anyList());
    }

    @Test
    @DisplayName("空目标车辆不产生任何写入")
    void batchCreate_noVehicles() {
        Task task = releasedTask(Instant.parse("2026-11-09T02:00:00Z"),
                Instant.parse("2026-11-09T01:00:00Z"), Instant.parse("2026-11-10T01:00:00Z"));

        assertEquals(0, repository.batchCreate(task, Set.of(), null));
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

        repository.batchCreate(task, Set.of(Vin.of("VIN001")), null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskVehiclePo>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskVehicleDao).batchInsertPo(captor.capture());
        TaskVehiclePo po = captor.getValue().get(0);
        assertNotNull(po.getReleaseAt());
        assertEquals(planned, po.getReleaseAt().toInstant());
    }

    @Test
    @DisplayName("CR-024 发布期冻结：snapshot_digest 与 tb_vehicle_task_snapshot 同批写入")
    void batchCreate_freezesManifestDigestAndSnapshot() {
        Task task = releasedTask(Instant.parse("2026-11-09T02:00:00Z"),
                Instant.parse("2026-11-09T01:00:00Z"), Instant.parse("2026-11-10T01:00:00Z"));
        VehicleTaskSnapshotSeal seal = VehicleTaskSnapshotSeal.builder()
                .snapshotDigest("a1b2c3")
                .canonicalizationVersion(1)
                .snapshotContent("{\"packages\":[]}")
                .build();

        when(taskVehicleDao.selectByTaskIdAndVin(anyLong(), anyString())).thenReturn(null);
        when(taskVehicleDao.batchInsertPo(anyList())).thenReturn(1);
        when(taskVehicleDao.selectIdVinByTaskId(9001L))
                .thenReturn(List.of(TaskVehiclePo.builder().id(77L).taskId(9001L).vin("VIN001").build()));
        when(vehicleTaskSnapshotDao.selectByVehicleTaskIdAndRevision(77L, 1L)).thenReturn(null);

        int created = repository.batchCreate(task, Set.of(Vin.of("VIN001")), seal);

        assertEquals(1, created);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskVehiclePo>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskVehicleDao).batchInsertPo(captor.capture());
        assertEquals("a1b2c3", captor.getValue().get(0).getSnapshotDigest());

        ArgumentCaptor<VehicleTaskSnapshotPo> snapshot = ArgumentCaptor.forClass(VehicleTaskSnapshotPo.class);
        verify(vehicleTaskSnapshotDao).insert(snapshot.capture());
        assertEquals(77L, snapshot.getValue().getVehicleTaskId());
        assertEquals(1L, snapshot.getValue().getTaskRevision());
        assertEquals("a1b2c3", snapshot.getValue().getSnapshotDigest());
        assertEquals("{\"packages\":[]}", snapshot.getValue().getSnapshotContent());
    }

    @Test
    @DisplayName("CR-024 冻结幂等：已存在同修订快照行不重复写入")
    void batchCreate_snapshotRowIdempotent() {
        Task task = releasedTask(Instant.parse("2026-11-09T02:00:00Z"),
                Instant.parse("2026-11-09T01:00:00Z"), Instant.parse("2026-11-10T01:00:00Z"));
        VehicleTaskSnapshotSeal seal = VehicleTaskSnapshotSeal.builder()
                .snapshotDigest("a1b2c3").canonicalizationVersion(1).snapshotContent("{}").build();

        when(taskVehicleDao.selectByTaskIdAndVin(anyLong(), anyString())).thenReturn(null);
        when(taskVehicleDao.batchInsertPo(anyList())).thenReturn(1);
        when(taskVehicleDao.selectIdVinByTaskId(9001L))
                .thenReturn(List.of(TaskVehiclePo.builder().id(77L).taskId(9001L).vin("VIN001").build()));
        when(vehicleTaskSnapshotDao.selectByVehicleTaskIdAndRevision(77L, 1L))
                .thenReturn(VehicleTaskSnapshotPo.builder().id(5L).vehicleTaskId(77L).taskRevision(1L).build());

        repository.batchCreate(task, Set.of(Vin.of("VIN001")), seal);

        verify(vehicleTaskSnapshotDao, never()).insert(org.mockito.ArgumentMatchers.any(VehicleTaskSnapshotPo.class));
    }

    @Test
    @DisplayName("CR-025 重置新建车辆任务：写入初始化口径与冻结摘要，并幂等写快照行")
    void createResetTarget_initializesAndFreezesSnapshot() {
        Instant release = Instant.parse("2026-11-15T02:00:00Z");
        Instant start = Instant.parse("2026-11-15T03:00:00Z");
        Instant end = Instant.parse("2026-11-16T03:00:00Z");
        VehicleTaskResetSeed seed = new VehicleTaskResetSeed(
                8L, 9001L, "VIN001", 77L,
                VehicleTaskStatus.DOWNLOAD_PENDING, false, ConsentState.NOT_REQUIRED,
                null, null, null,
                release, start, end,
                1L, "a1b2c3", "tester");

        when(vehicleTaskSnapshotDao.selectByVehicleTaskIdAndRevision(77L, 1L))
                .thenReturn(VehicleTaskSnapshotPo.builder().id(5L).vehicleTaskId(77L).taskRevision(1L)
                        .snapshotDigest("a1b2c3").snapshotContent("{\"packages\":[]}").build());
        when(vehicleTaskSnapshotDao.selectByVehicleTaskIdAndRevision(90L, 1L)).thenReturn(null);
        when(taskVehicleDao.insertResetTarget(org.mockito.ArgumentMatchers.any(TaskVehiclePo.class)))
                .thenAnswer(inv -> {
                    TaskVehiclePo po = inv.getArgument(0);
                    po.setId(90L);
                    return 1;
                });

        Long newId = repository.createResetTarget(seed);

        assertEquals(90L, newId);
        ArgumentCaptor<TaskVehiclePo> po = ArgumentCaptor.forClass(TaskVehiclePo.class);
        verify(taskVehicleDao).insertResetTarget(po.capture());
        TaskVehiclePo target = po.getValue();
        assertEquals(9001L, target.getTaskId());
        assertEquals("VIN001", target.getVin());
        assertEquals(VehicleTaskStatus.DOWNLOAD_PENDING.getValue(), target.getVehicleTaskStatus());
        assertEquals(DownloadReadyState.NOT_STARTED.getValue(), target.getDownloadReadyState());
        assertEquals(1L, target.getTaskRevision());
        assertEquals("a1b2c3", target.getSnapshotDigest());
        assertEquals(0, target.getLastAttemptNo());
        assertEquals(0, target.getConsentRequired());
        assertNull(target.getSupersededBy(), "新车辆任务不得带取代者");
        assertEquals("tester", target.getCreateBy());
        assertEquals(release, target.getReleaseAt().toInstant());
        assertEquals(start, target.getVtStartTime().toInstant());
        assertEquals(end, target.getVtEndTime().toInstant());
        assertTrue(target.getDescription().contains("77"));

        ArgumentCaptor<VehicleTaskSnapshotPo> snapshot = ArgumentCaptor.forClass(VehicleTaskSnapshotPo.class);
        verify(vehicleTaskSnapshotDao).insert(snapshot.capture());
        assertEquals(90L, snapshot.getValue().getVehicleTaskId());
        assertEquals(1L, snapshot.getValue().getTaskRevision());
        assertEquals("a1b2c3", snapshot.getValue().getSnapshotDigest());
        assertEquals("{\"packages\":[]}", snapshot.getValue().getSnapshotContent(),
                "快照内容继承原车辆任务（同一任务同一冻结摘要）");
    }

    @Test
    @DisplayName("CR-025 重置新建车辆任务：父任务无冻结摘要时保持 NULL 且不写快照行")
    void createResetTarget_withoutDigest_skipsSnapshot() {
        VehicleTaskResetSeed seed = new VehicleTaskResetSeed(
                8L, 9001L, "VIN001", 77L,
                VehicleTaskStatus.DOWNLOAD_PENDING, false, ConsentState.NOT_REQUIRED,
                null, null, null, Instant.now(), Instant.now(), Instant.now().plusSeconds(60),
                1L, null, "tester");
        when(taskVehicleDao.insertResetTarget(org.mockito.ArgumentMatchers.any(TaskVehiclePo.class)))
                .thenAnswer(inv -> {
                    TaskVehiclePo po = inv.getArgument(0);
                    po.setId(91L);
                    return 1;
                });

        Long newId = repository.createResetTarget(seed);

        assertEquals(91L, newId);
        verify(vehicleTaskSnapshotDao, never()).insert(org.mockito.ArgumentMatchers.any(VehicleTaskSnapshotPo.class));
    }
}
