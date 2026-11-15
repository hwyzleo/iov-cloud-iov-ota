package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.VehicleTaskSnapshotSeal;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 包清单摘要存量回填测试（CR-024 §4.28.3.4）。
 *
 * <p>回填由应用侧幂等执行：只填充空摘要、已有值不改写、单任务失败不中断整体。</p>
 *
 * @author hwyz_leo
 */
@DisplayName("ManifestDigestBackfillService - 存量摘要回填")
class ManifestDigestBackfillServiceTest {

    private TaskVehicleRepository taskVehicleRepository;
    private TaskRepository taskRepository;
    private VehicleTaskPackageService vehicleTaskPackageService;
    private PackageManifestDigestService digestService;
    private ManifestDigestBackfillService service;

    @BeforeEach
    void setUp() {
        taskVehicleRepository = mock(TaskVehicleRepository.class);
        taskRepository = mock(TaskRepository.class);
        vehicleTaskPackageService = mock(VehicleTaskPackageService.class);
        digestService = mock(PackageManifestDigestService.class);
        service = new ManifestDigestBackfillService(taskVehicleRepository, taskRepository,
                vehicleTaskPackageService, digestService);
    }

    private Task task() {
        return Task.create(TaskId.of(9001L), "回填任务", TaskType.NORMAL, ActivityId.of(100L));
    }

    @Test
    @DisplayName("无待回填任务 -> 不做任何写入")
    void backfillAll_nothingToDo() {
        when(taskVehicleRepository.taskIdsWithoutSnapshotDigest()).thenReturn(List.of());

        assertEquals(0, service.backfillAll());
        verify(taskVehicleRepository, never()).backfillSnapshot(anyLong(), any());
    }

    @Test
    @DisplayName("按任务回填：摘要来自发布期同口径投影，写入 vehicle_task 与快照行")
    void backfillAll_backfillsEachTask() {
        when(taskVehicleRepository.taskIdsWithoutSnapshotDigest()).thenReturn(List.of(9001L, 9002L));
        when(taskRepository.getById(any())).thenAnswer(inv -> {
            TaskId id = inv.getArgument(0);
            Task t = task();
            return id.getValue() == 9001L ? Optional.of(t) : Optional.empty();
        });
        when(vehicleTaskPackageService.projectForTask(any())).thenReturn(List.<DetectionResult.MatchedPackage>of());
        when(digestService.seal(any(), eq("task-9001-r1"), eq(9001L)))
                .thenReturn(VehicleTaskSnapshotSeal.builder()
                        .snapshotDigest("frozen-digest").canonicalizationVersion(1).snapshotContent("{}").build());
        when(taskVehicleRepository.backfillSnapshot(eq(9001L), any())).thenReturn(3);

        assertEquals(3, service.backfillAll());
        verify(taskVehicleRepository).backfillSnapshot(eq(9001L), any());
        // 第二个任务不存在：跳过，不写
        verify(taskVehicleRepository, never()).backfillSnapshot(eq(9002L), any());
    }

    @Test
    @DisplayName("单任务异常不中断整体回填")
    void backfillAll_continuesOnFailure() {
        when(taskVehicleRepository.taskIdsWithoutSnapshotDigest()).thenReturn(List.of(9001L, 9002L));
        when(taskRepository.getById(any())).thenAnswer(inv -> Optional.of(task()));
        when(vehicleTaskPackageService.projectForTask(any())).thenReturn(List.<DetectionResult.MatchedPackage>of());
        when(digestService.seal(any(), any(), eq(9001L))).thenThrow(new IllegalStateException("boom"));
        when(digestService.seal(any(), any(), eq(9002L)))
                .thenReturn(VehicleTaskSnapshotSeal.builder()
                        .snapshotDigest("frozen-digest").canonicalizationVersion(1).snapshotContent("{}").build());
        when(taskVehicleRepository.backfillSnapshot(eq(9002L), any())).thenReturn(1);

        assertEquals(1, service.backfillAll());
        verify(taskVehicleRepository, never()).backfillSnapshot(eq(9001L), any());
        verify(taskVehicleRepository).backfillSnapshot(eq(9002L), any());
    }

    @Test
    @DisplayName("回填幂等：仓库返回 0（已有值）时不影响结果")
    void backfillTask_returnsUpdatedCount() {
        when(taskRepository.getById(any())).thenReturn(Optional.of(task()));
        when(vehicleTaskPackageService.projectForTask(any())).thenReturn(List.<DetectionResult.MatchedPackage>of());
        when(digestService.seal(any(), any(), anyLong()))
                .thenReturn(VehicleTaskSnapshotSeal.builder()
                        .snapshotDigest("frozen-digest").canonicalizationVersion(1).snapshotContent("{}").build());
        when(taskVehicleRepository.backfillSnapshot(eq(9001L), any())).thenReturn(0);

        assertEquals(0, service.backfillTask(9001L));
    }
}
