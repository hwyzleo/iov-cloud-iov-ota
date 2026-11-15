package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.VehicleTaskSnapshotSeal;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskResetSeed;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.TaskVehiclePoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskSnapshotMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskSnapshotPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 升级任务车辆仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskVehicleRepositoryImpl extends AbstractRepository<Long, TaskVehicleDo> implements TaskVehicleRepository {

    private final TaskVehicleMapper taskVehicleDao;
    private final VehicleTaskSnapshotMapper vehicleTaskSnapshotDao;

    @Override
    public Optional<TaskVehicleDo> getById(Long id) {
        TaskVehiclePo taskVehiclePo = taskVehicleDao.selectPoById(id);
        return Optional.ofNullable(TaskVehiclePoAssembler.INSTANCE.toDo(taskVehiclePo));
    }

    @Override
    public Optional<TaskVehicleDo> getByTaskIdAndVin(Long taskId, String vin) {
        TaskVehiclePo taskVehiclePo = taskVehicleDao.selectByTaskIdAndVin(taskId, vin);
        return Optional.ofNullable(TaskVehiclePoAssembler.INSTANCE.toDo(taskVehiclePo));
    }

    @Override
    public boolean save(TaskVehicleDo taskVehicleDo) {
        switch (taskVehicleDo.getState()) {
            case CHANGED -> {
                TaskVehiclePo taskVehiclePo = TaskVehiclePoAssembler.INSTANCE.fromDo(taskVehicleDo);
                taskVehicleDao.updatePo(taskVehiclePo);
                // 落库后复位脏标记，避免领域对象残留 CHANGED 状态
                taskVehicleDo.statePersistent(taskVehicleDo.getId());
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * 发布时生成 VehicleTask 快照（CR-012 §2.2 / US-057）。
     *
     * <p>除 activityId/taskId/vin 外，必须同时初始化 VehicleTask 运行态字段：
     * <ul>
     *   <li>vehicle_task_status：发布即可进入下载准备（请求不要求授权时），否则读取侧
     *       {@code vehicle_task_status IS NOT NULL} 过滤会丢弃该行，车端检测不到任务；</li>
     *   <li>release_at / vt_start_time / vt_end_time：发布时冻结的发布时刻与执行窗口快照；</li>
     *   <li>task_revision：初始修订号 1；</li>
     *   <li>download_ready_state / consent_state / consent_required：初始下载与授权态。</li>
     * </ul>
     *
     * <p>备注：CR-016 要求按活动冻结条款（articleId/version/hash）并把授权态置为 PENDING；
     * 当前发布链路尚未接入条款摘要下发，故先按“无需授权”冻结，避免因冻结 hash 缺失导致
     * 安装许可被 ConsentPolicy 永久阻断（保持既有有效行为）。</p>
     *
     * <p>CR-024 §4.28.3：发布时同事务写入 {@code snapshot_digest} 与 {@code tb_vehicle_task_snapshot}，
     * 摘要由应用层 {@code PackageManifestDigestService} 在发布期一次性冻结；快照行按
     * {@code (vehicle_task_id, task_revision)} 幂等，已存在则跳过。摘要缺失时仍照常创建
     * 车辆任务（存量兼容），由安装许可 fail-open 兵接。</p>
     */
    @Override
    public int batchCreate(Task task, Set<Vin> vehicles, VehicleTaskSnapshotSeal seal) {
        if (task == null || task.getId() == null || vehicles == null || vehicles.isEmpty()) {
            return 0;
        }
        Long taskId = task.getId().getValue();
        Long activityId = task.getActivityId() != null ? task.getActivityId().getValue() : null;
        Date releaseAt = toDate(resolveReleaseAt(task));
        Date startTime = toDate(task.getStartTime());
        Date endTime = toDate(task.getEndTime());

        List<TaskVehiclePo> newRecords = new ArrayList<>();
        for (Vin vin : vehicles) {
            TaskVehiclePo existing = taskVehicleDao.selectByTaskIdAndVin(taskId, vin.getValue());
            if (existing != null) {
                continue;
            }
            newRecords.add(TaskVehiclePo.builder()
                    .activityId(activityId)
                    .taskId(taskId)
                    .vin(vin.getValue())
                    .vehicleTaskStatus(VehicleTaskStatus.DOWNLOAD_PENDING.getValue())
                    .taskRevision(TaskRevision.initial().getValue())
                    .downloadReadyState(DownloadReadyState.NOT_STARTED.getValue())
                    .consentRequired(0)
                    .consentState(ConsentState.NOT_REQUIRED.getValue())
                    .snapshotDigest(seal == null ? null : seal.getSnapshotDigest())
                    .releaseAt(releaseAt)
                    .vtStartTime(startTime)
                    .vtEndTime(endTime)
                    .lastAttemptNo(0)
                    .build());
        }
        if (newRecords.isEmpty()) {
            return 0;
        }
        int created = taskVehicleDao.batchInsertPo(newRecords);
        freezeSnapshots(taskId, seal);
        log.info("任务[{}]发布快照写入车辆任务[{}]条（发布时刻[{}]，执行窗口[{},{}]，包清单摘要[{}]）",
                taskId, created, releaseAt, startTime, endTime,
                seal == null ? null : seal.getSnapshotDigest());
        return created;
    }

    /**
     * 写入 {@code tb_vehicle_task_snapshot}（按 vehicleTaskId + taskRevision 幂等）。     *
     * <p>车辆任务主键由批量插入生成，因此插入后按 taskId 一次回查 vin → id 映射，避免 N+1。</p>
     */
    private void freezeSnapshots(Long taskId, VehicleTaskSnapshotSeal seal) {
        if (seal == null || seal.getSnapshotDigest() == null) {
            return;
        }
        Map<String, Long> vehicleTaskIds = vehicleTaskIds(taskId);
        if (vehicleTaskIds.isEmpty()) {
            return;
        }
        Long revision = TaskRevision.initial().getValue();
        for (Long vehicleTaskId : vehicleTaskIds.values()) {
            writeSnapshotRow(vehicleTaskId, revision, seal.getSnapshotDigest(),
                    seal.getSnapshotContent() == null ? "{}" : seal.getSnapshotContent());
        }
        log.info("任务[{}]冻结包清单快照[{}]条（摘要[{}]，规范化版本[{}]）",
                taskId, vehicleTaskIds.size(), seal.getSnapshotDigest(), seal.getCanonicalizationVersion());
    }

    /**
     * 幂等写入单条包清单冻结快照（按 vehicleTaskId + taskRevision）。
     */
    private void writeSnapshotRow(Long vehicleTaskId, Long revision, String digest, String content) {
        if (vehicleTaskId == null || revision == null || digest == null) {
            return;
        }
        VehicleTaskSnapshotPo existing =
                vehicleTaskSnapshotDao.selectByVehicleTaskIdAndRevision(vehicleTaskId, revision);
        if (existing != null) {
            return;
        }
        vehicleTaskSnapshotDao.insert(VehicleTaskSnapshotPo.builder()
                .vehicleTaskId(vehicleTaskId)
                .taskRevision(revision)
                .snapshotDigest(digest)
                .snapshotContent(content == null ? "{}" : content)
                .build());
    }

    @Override
    public Long createResetTarget(VehicleTaskResetSeed seed) {
        if (seed == null || seed.taskId() == null || seed.vin() == null) {
            return null;
        }
        Long revision = seed.taskRevision() != null
                ? seed.taskRevision() : TaskRevision.initial().getValue();
        TaskVehiclePo po = TaskVehiclePo.builder()
                .activityId(seed.activityId())
                .taskId(seed.taskId())
                .vin(seed.vin())
                .vehicleTaskStatus(seed.status() != null ? seed.status().getValue() : null)
                .taskRevision(revision)
                .snapshotDigest(seed.snapshotDigest())
                .downloadReadyState(DownloadReadyState.NOT_STARTED.getValue())
                .consentRequired(seed.consentRequired() ? 1 : 0)
                .consentState(seed.consentState() != null ? seed.consentState().getValue() : null)
                .consentArticleId(seed.consentArticleId())
                .consentArticleVersion(seed.consentArticleVersion())
                .consentArticleHash(seed.consentArticleHash())
                .releaseAt(toDate(seed.releaseAt()))
                .vtStartTime(toDate(seed.startTime()))
                .vtEndTime(toDate(seed.endTime()))
                .lastAttemptNo(0)
                .description("重置新建（原车辆任务[" + seed.sourceVehicleTaskId() + "]）")
                .createBy(seed.operator())
                .modifyBy(seed.operator())
                .build();
        taskVehicleDao.insertResetTarget(po);
        Long newVehicleTaskId = po.getId();
        // 复用父任务冻结摘要：快照内容继承旧行（存在时），否则空 JSON 占位；不回写摘要本身
        String snapshotContent = "{}";
        if (seed.sourceVehicleTaskId() != null && seed.snapshotDigest() != null) {
            VehicleTaskSnapshotPo source =
                    vehicleTaskSnapshotDao.selectByVehicleTaskIdAndRevision(seed.sourceVehicleTaskId(), revision);
            if (source != null && source.getSnapshotContent() != null) {
                snapshotContent = source.getSnapshotContent();
            }
        }
        writeSnapshotRow(newVehicleTaskId, revision, seed.snapshotDigest(), snapshotContent);
        log.info("车辆任务重置：为任务[{}]车辆[{}]新建车辆任务[{}]（原车辆任务[{}]，摘要[{}]）",
                seed.taskId(), seed.vin(), newVehicleTaskId, seed.sourceVehicleTaskId(), seed.snapshotDigest());
        return newVehicleTaskId;
    }

    /**
     * 任务下车辆任务的 vin → vehicleTaskId 映射。
     */
    private Map<String, Long> vehicleTaskIds(Long taskId) {
        List<TaskVehiclePo> rows = taskVehicleDao.selectIdVinByTaskId(taskId);
        Map<String, Long> map = new java.util.HashMap<>();
        if (rows == null) {
            return map;
        }
        for (TaskVehiclePo po : rows) {
            if (po.getId() != null && po.getVin() != null) {
                map.put(po.getVin(), po.getId());
            }
        }
        return map;
    }

    @Override
    public List<Long> taskIdsWithoutSnapshotDigest() {
        List<Long> taskIds = taskVehicleDao.selectTaskIdsWithoutSnapshotDigest();
        return taskIds == null ? List.of() : taskIds;
    }

    @Override
    public int backfillSnapshot(Long taskId, VehicleTaskSnapshotSeal seal) {
        if (taskId == null || seal == null || seal.getSnapshotDigest() == null) {
            return 0;
        }
        int updated = taskVehicleDao.updateSnapshotDigestByTaskId(taskId, seal.getSnapshotDigest());
        freezeSnapshots(taskId, seal);
        return updated;
    }

    /**
     * 发布时刻快照：优先实际发布时间，其次计划发布时间，最后兜底当前时间。
     */
    private Instant resolveReleaseAt(Task task) {
        if (task.getActualReleaseTime() != null) {
            return task.getActualReleaseTime();
        }
        if (task.getReleaseTime() != null) {
            return task.getReleaseTime();
        }
        return Instant.now();
    }

    private Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }
}
