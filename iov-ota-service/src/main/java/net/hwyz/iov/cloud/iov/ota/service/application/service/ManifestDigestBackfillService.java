package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.VehicleTaskSnapshotSeal;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 包清单摘要存量回填（CR-024 §4.28.3.4）。
 *
 * <p>摘要必须按 {@link PackageManifestDigestService} 对投影字节做 SHA-256，MySQL 侧无法等价实现，
 * 因此不使用 Flyway 数据脚本，而是应用侧幂等回填（由开关控制的启动任务或运维调用触发）。</p>
 *
 * <p>幂等约束：只更新 {@code snapshot_digest} 仍为空的车辆任务行；已有值一律不改写，
 * 避免以新算法或新投影覆盖历史冻结值（摘要空间按 canonicalization 版本隔离）。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManifestDigestBackfillService {

    private final TaskVehicleRepository taskVehicleRepository;
    private final TaskRepository taskRepository;
    private final VehicleTaskPackageService vehicleTaskPackageService;
    private final PackageManifestDigestService packageManifestDigestService;

    /**
     * 执行一次全量回填（幂等）。
     *
     * @return 实际回填的车辆任务数
     */
    public int backfillAll() {
        var taskIds = taskVehicleRepository.taskIdsWithoutSnapshotDigest();
        if (taskIds == null || taskIds.isEmpty()) {
            log.info("包清单摘要回填：无待回填任务");
            return 0;
        }
        int total = 0;
        int taskFailed = 0;
        for (Long taskId : taskIds) {
            try {
                total += backfillTask(taskId);
            } catch (RuntimeException e) {
                taskFailed++;
                log.error("包清单摘要回填失败：taskId[{}]", taskId, e);
            }
        }
        log.info("包清单摘要回填完成：任务数[{}]，失败任务数[{}]，回填车辆任务数[{}]",
                taskIds.size(), taskFailed, total);
        return total;
    }

    /**
     * 回填单个任务（幂等）。
     *
     * @param taskId 任务ID
     * @return 受影响车辆任务数
     */
    public int backfillTask(Long taskId) {
        Optional<Task> taskOpt = taskRepository.getById(TaskId.of(taskId));
        if (taskOpt.isEmpty()) {
            log.warn("包清单摘要回填跳过：任务[{}]不存在", taskId);
            return 0;
        }
        Task task = taskOpt.get();
        VehicleTaskSnapshotSeal seal = packageManifestDigestService.seal(
                vehicleTaskPackageService.projectForTask(task),
                installPlanVersion(taskId), taskId);
        int updated = taskVehicleRepository.backfillSnapshot(taskId, seal);
        if (updated > 0) {
            log.info("包清单摘要回填：taskId[{}] 车辆任务[{}]条，摘要[{}]",
                    taskId, updated, seal.getSnapshotDigest());
        }
        return updated;
    }

    /**
     * 安装计划版本：与发布冻结、TaskCheck 下发、许可重算同口径（回填任务均为初始修订号）。
     */
    private static String installPlanVersion(Long taskId) {
        return "task-" + taskId + "-r"
                + net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision.initial().getValue();
    }
}
