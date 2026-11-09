package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.TaskVehiclePoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
     */
    @Override
    public int batchCreate(Task task, Set<Vin> vehicles) {
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
        log.info("任务[{}]发布快照写入车辆任务[{}]条（发布时刻[{}]，执行窗口[{},{}]）",
                taskId, created, releaseAt, startTime, endTime);
        return created;
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
