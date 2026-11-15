package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.VehicleTaskResetCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskResetResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.VehicleTaskResetException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskResetSeed;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleResetLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleResetLogPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

/**
 * 车辆任务重置应用服务（IOV-OTA-DSN-CR-025 §4.29.5）。
 *
 * <p>语义：旧车辆任务强制关闭为 SUPERSEDED（记录取代者）→ 同任务同 VIN 新建一条
 * 初始化状态的车辆任务。不物理删除任何事实：授权历史、安装尝试、事件、日志、
 * 包清单冻结快照全部保留。</p>
 *
 * <p>并发：先锁活动行（同一活动内串行化），再对车辆任务行加行锁；同一车辆任务的
 * 重复/并发重置只有一次成功，其余以 {@code 403007} 拒绝。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTaskResetAppService {

    /** 重置原因最大长度（与 tb_task_vehicle_reset_log.reason 一致） */
    private static final int REASON_MAX_LENGTH = 512;
    /** 重置时下发给车端的本地任务处置意图 */
    private static final String LOCAL_DISPOSITION_ABANDON = "ABANDON";
    /** 重置默认包缓存处置意图：不因重置删除已下载包 */
    private static final String PACKAGE_CACHE_KEEP = "KEEP";

    private final TaskRepository taskRepository;
    private final VehicleTaskRepository vehicleTaskRepository;
    private final TaskVehicleRepository taskVehicleRepository;
    private final ExecutionRepository executionRepository;
    private final TaskVehicleResetLogMapper resetLogMapper;

    /**
     * 重置车辆任务。
     *
     * @param cmd 重置命令
     * @return 重置结果（原/新车辆任务ID、是否强制收口活动执行）
     */
    @Transactional
    public VehicleTaskResetResult reset(VehicleTaskResetCmd cmd) {
        validateReason(cmd);
        Long vehicleTaskId = cmd.getVehicleTaskId();
        log.info("管理后台用户[{}]重置车辆任务[{}]，强制[{}]", cmd.getOperator(), vehicleTaskId, cmd.isForce());

        // 1. 预读：取得活动ID并以活动行锁串行化（同一活动内的重置操作串行化）
        VehicleTask preview = vehicleTaskRepository.getById(VehicleTaskId.of(vehicleTaskId))
                .orElseThrow(() -> VehicleTaskResetException.notCurrent(vehicleTaskId));
        Task previewTask = taskRepository.getById(TaskId.of(preview.getTaskId())).orElse(null);
        if (previewTask != null && previewTask.getActivityId() != null) {
            taskRepository.lockActivity(previewTask.getActivityId().getValue());
        }

        // 2. 行锁读取车辆任务（串行化同一车辆任务的并发重置）
        VehicleTask vt = vehicleTaskRepository.getByIdForUpdate(VehicleTaskId.of(vehicleTaskId))
                .orElseThrow(() -> VehicleTaskResetException.notCurrent(vehicleTaskId));
        assertResettable(vt, cmd);

        // 3. 父任务守卫
        Task task = taskRepository.getById(TaskId.of(vt.getTaskId()))
                .orElseThrow(() -> VehicleTaskResetException.taskStateBlocked(vt.getTaskId(), "NOT_EXIST"));
        Instant now = Instant.now();
        assertTaskGuard(task, now);

        // 4. 当前性复核（防止与其它当前行并存）
        Optional<VehicleTask> current = vehicleTaskRepository.getByTaskIdAndVin(
                TaskId.of(vt.getTaskId()), vt.getVin());
        if (current.isEmpty() || !current.get().getId().getValue().equals(vehicleTaskId)) {
            throw VehicleTaskResetException.notCurrent(vehicleTaskId);
        }

        // 5. 活动 Execution 处置
        Execution activeExecution = executionRepository
                .findActiveByVehicleTaskId(VehicleTaskId.of(vehicleTaskId)).orElse(null);
        boolean forcedCanceled = false;
        Long oldExecutionId = null;
        String oldExecutionStatus = null;
        if (activeExecution != null) {
            if (!cmd.isForce()) {
                throw VehicleTaskResetException.needForce(vehicleTaskId, activeExecution.getId().getValue());
            }
            oldExecutionId = activeExecution.getId().getValue();
            oldExecutionStatus = activeExecution.getStatus() != null
                    ? activeExecution.getStatus().getValue() : null;
            // 既有合法跃迁：PERMITTED/INSTALLING/PAUSED/ROLLING_BACK -> CANCELED；
            // save 内部会删除 tb_task_vehicle_execution_active 占位
            activeExecution.cancel();
            executionRepository.save(activeExecution);
            forcedCanceled = true;
            log.warn("车辆任务[{}]重置：强制收口活动执行[{}]（原状态[{}]），车端后续迟到事件将被拒绝",
                    vehicleTaskId, oldExecutionId, oldExecutionStatus);
        }

        // 6. 新建初始化车辆任务（沿用发布建档口径，复用父任务冻结摘要，不继承授权）
        VehicleTaskResetSeed seed = buildSeed(vt, task, cmd.getOperator());
        Long newVehicleTaskId = taskVehicleRepository.createResetTarget(seed);
        if (newVehicleTaskId == null) {
            throw new IllegalStateException("车辆任务[" + vehicleTaskId + "]重置失败：新建车辆任务未取得主键");
        }

        // 7. 关闭旧车辆任务：处置意图 + SUPERSEDED + 取代者
        vt.prepareResetDisposition(LOCAL_DISPOSITION_ABANDON, PACKAGE_CACHE_KEEP);
        vt.supersede(VehicleTaskId.of(newVehicleTaskId));
        vehicleTaskRepository.save(vt);

        // 8. 不可变审计（成功重置追加一条）
        resetLogMapper.insert(buildResetLog(vt, task, newVehicleTaskId, cmd, oldExecutionId, oldExecutionStatus));

        log.info("车辆任务[{}]已重置为新车辆任务[{}]（任务[{}]，车辆[{}]，强制收口[{}]）",
                vehicleTaskId, newVehicleTaskId, vt.getTaskId(), vt.getVin(), forcedCanceled);
        return VehicleTaskResetResult.builder()
                .oldVehicleTaskId(vehicleTaskId)
                .newVehicleTaskId(newVehicleTaskId)
                .oldStatus(VehicleTaskStatus.SUPERSEDED.getValue())
                .forcedExecutionCanceled(forcedCanceled)
                .oldExecutionId(oldExecutionId)
                .oldExecutionStatus(oldExecutionStatus)
                .build();
    }

    private void validateReason(VehicleTaskResetCmd cmd) {
        if (cmd == null || cmd.getVehicleTaskId() == null) {
            throw VehicleTaskResetException.notCurrent(null);
        }
        if (!StringUtils.hasText(cmd.getReason()) || cmd.getReason().length() > REASON_MAX_LENGTH) {
            throw VehicleTaskResetException.reasonRequired();
        }
    }

    /**
     * 车辆任务准入：必须是当前车辆任务，且状态允许重置（除 SUCCEEDED 外全部允许）。
     */
    private void assertResettable(VehicleTask vt, VehicleTaskResetCmd cmd) {
        Long vehicleTaskId = vt.getId().getValue();
        if (vt.getSupersededBy() != null || vt.getStatus() == VehicleTaskStatus.SUPERSEDED) {
            throw VehicleTaskResetException.notCurrent(vehicleTaskId);
        }
        if (vt.getStatus() == VehicleTaskStatus.SUCCEEDED) {
            throw VehicleTaskResetException.succeededNotResettable(vehicleTaskId);
        }
        if (vt.getStatus() == null) {
            throw VehicleTaskResetException.notCurrent(vehicleTaskId);
        }
        if (cmd.getRowVersion() != null && cmd.getRowVersion() != vt.getRowVersion()) {
            throw VehicleTaskResetException.conflict(vehicleTaskId);
        }
    }

    /**
     * 父任务守卫（fail-safe）：仅 RELEASED / IN_PROGRESS 且未过 endTime 允许重置。
     */
    private void assertTaskGuard(Task task, Instant now) {
        TaskState state = task.getState();
        if (state != TaskState.RELEASED && state != TaskState.IN_PROGRESS) {
            throw VehicleTaskResetException.taskStateBlocked(task.getId().getValue(),
                    state == null ? "NULL" : state.name());
        }
        if (task.getEndTime() != null && !now.isBefore(task.getEndTime())) {
            throw VehicleTaskResetException.windowExpired(task.getId().getValue());
        }
    }

    private VehicleTaskResetSeed buildSeed(VehicleTask vt, Task task, String operator) {
        boolean consentRequired = vt.isConsentRequired();
        return new VehicleTaskResetSeed(
                task.getActivityId() != null ? task.getActivityId().getValue() : null,
                vt.getTaskId(),
                vt.getVin(),
                vt.getId().getValue(),
                consentRequired ? VehicleTaskStatus.CONSENT_PENDING : VehicleTaskStatus.DOWNLOAD_PENDING,
                consentRequired,
                consentRequired ? ConsentState.PENDING : ConsentState.NOT_REQUIRED,
                vt.getConsentArticleId(),
                vt.getConsentArticleVersion(),
                vt.getConsentArticleHash(),
                vt.getReleaseAt(),
                vt.getStartTime(),
                vt.getEndTime(),
                TaskRevision.initial().getValue(),
                vt.getSnapshotDigest() != null ? vt.getSnapshotDigest().getValue() : null,
                operator);
    }

    private TaskVehicleResetLogPo buildResetLog(VehicleTask vt, Task task, Long newVehicleTaskId,
                                                VehicleTaskResetCmd cmd, Long oldExecutionId,
                                                String oldExecutionStatus) {
        return TaskVehicleResetLogPo.builder()
                .oldVehicleTaskId(vt.getId().getValue())
                .newVehicleTaskId(newVehicleTaskId)
                .taskId(vt.getTaskId())
                .activityId(task.getActivityId() != null ? task.getActivityId().getValue() : null)
                .vin(vt.getVin())
                .oldStatus(VehicleTaskStatus.SUPERSEDED.getValue())
                .oldDownloadReady(vt.getDownloadReadyState() != null ? vt.getDownloadReadyState().getValue() : null)
                .oldConsentState(vt.getConsentState() != null ? vt.getConsentState().getValue() : null)
                .oldAttemptNo(vt.getLastAttemptNo())
                .oldExecutionId(oldExecutionId)
                .oldExecutionStatus(oldExecutionStatus)
                .forceFlag(cmd.isForce() ? 1 : 0)
                .reason(cmd.getReason())
                .result("SUCCESS")
                .createBy(cmd.getOperator())
                .modifyBy(cmd.getOperator())
                .build();
    }
}
