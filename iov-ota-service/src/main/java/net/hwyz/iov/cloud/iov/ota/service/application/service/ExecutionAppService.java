package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionFinalizeCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.EcuResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.SoftwareUnitResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionCreateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionFinalizeResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.OutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ConsentPolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.InstallPermitService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEcuResultMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEcuSoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEcuResultPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEcuSoftwareUnitPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 安装执行应用服务（CR-012 §5.5/§5.7、US-079/US-081）
 *
 * <p>安装门禁、Execution 幂等创建、许可签发、最终结果收口。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionAppService {

    private final VehicleTaskRepository vehicleTaskRepository;
    private final ExecutionRepository executionRepository;
    private final TaskRepository taskRepository;
    private final InstallPermitService installPermitService;
    private final ConsentPolicy consentPolicy;
    private final VehicleTaskConsentRepository vehicleTaskConsentRepository;
    private final OutboxRepository outboxRepository;
    private final ExecutionEcuResultMapper executionEcuResultMapper;
    private final ExecutionEcuSoftwareUnitMapper executionEcuSoftwareUnitMapper;
    private final VehicleTaskPackageService vehicleTaskPackageService;
    private final PackageManifestDigestService packageManifestDigestService;
    private final RetryTrajectoryService retryTrajectoryService;

    /** 简易 ID 生成器（TODO: 替换为雪花算法或 DB 序列） */
    private final AtomicLong idSeed = new AtomicLong(System.currentTimeMillis() / 1000);

    /**
     * 申请安装许可，创建 Execution（CR-012 §5.5）。
     *
     * <p><b>noRollbackFor 的原因</b>：前置守卫不满足属于确定性业务拒绝，调用方
     * （{@code InstallPermitCommandHandler}）会捕获 {@link ExecutionStateException} 并回
     * {@code allowed=false + deny_reason} 的 RESPONSE。若此处按默认规则回滚，加入同一物理事务的
     * 上层（Kafka 入站流水线）会被标记 rollback-only，catch 之后提交阶段抛
     * {@code UnexpectedRollbackException}，拒绝响应连同 Outbox 一起回滚，车端永远收不到答复、
     * offset 反复重投直至隔离（实车 2026-11-15 任务 4 {@code DOWNLOAD_PENDING} 场景）。
     *
     * @param cmd 创建命令
     * @return 创建结果（含许可令牌）
     * @throws ExecutionStateException 前置守卫不满足（业务拒绝，不回滚外层事务）
     */
    @Transactional(noRollbackFor = ExecutionStateException.class)
    public ExecutionCreateResult requestInstall(ExecutionCreateCmd cmd) {
        log.info("车辆[{}]申请安装许可，车辆任务[{}]", cmd.getVin(), cmd.getVehicleTaskId());

        VehicleTask vehicleTask = vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId()))
                .orElseThrow(() -> new ExecutionStateException("车辆任务[" + cmd.getVehicleTaskId() + "]不存在"));
        Task task = taskRepository.getById(TaskId.of(vehicleTask.getTaskId()))
                .orElseThrow(() -> new ExecutionStateException("任务[" + vehicleTask.getTaskId() + "]不存在"));

        ExecutionId executionId = ExecutionId.of(generateId());
        Instant validUntil = Instant.now().plusSeconds(1800);

        // 授权门禁：统一 ConsentPolicy 判定（CR-016 §4）
        boolean consentRequired = vehicleTask.isConsentRequired();
        boolean consentPermitted = !consentRequired || consentPolicy.isPermitted(
                vehicleTask,
                vehicleTaskConsentRepository.findCurrentByVehicleTaskId(vehicleTask.getId().getValue()).orElse(null),
                Instant.now());

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .installPlanVersion(cmd.getInstallPlanVersion())
                .packageManifestDigest(cmd.getPackageManifestDigest() != null
                        ? SnapshotDigest.of(cmd.getPackageManifestDigest()) : null)
                .expectedPackageManifestDigest(cmd.getExpectedPackageManifestDigest() != null
                        ? SnapshotDigest.of(cmd.getExpectedPackageManifestDigest()) : null)
                .conditionSetVersion(cmd.getConditionSetVersion())
                .validUntil(validUntil)
                .offlinePolicy(cmd.getOfflinePolicy())
                .timeoutPolicy(cmd.getTimeoutPolicy())
                .controlPolicy(cmd.getControlPolicy())
                .consentRequired(consentRequired)
                .consentPermitted(consentPermitted)
                .allPackageStageResultsSucceeded(true)
                .build();

        Execution execution = installPermitService.requestInstall(executionId, task, vehicleTask, request,
                recomputeManifestDigest(vehicleTask, task), Instant.now());

        executionRepository.save(execution);
        vehicleTaskRepository.save(vehicleTask);

        outboxRepository.append("EXECUTION", String.valueOf(executionId.getValue()),
                "ExecutionCreated", "{}");

        // IOV-OTA-DSN-CR-026 §7.4：安装执行创建/安装重试形成不可变业务轨迹（同事务）
        retryTrajectoryService.recordExecutionCreated(vehicleTask, execution.getAttemptNo(), Instant.now());

        return ExecutionCreateResult.builder()
                .executionId(executionId.getValue())
                .attemptNo(execution.getAttemptNo())
                .permitToken(execution.getPermitToken().getToken())
                .validUntil(validUntil)
                .taskRevision(execution.getTaskRevision().getValue())
                .installPlanVersion(execution.getInstallPlanVersion())
                .build();
    }

    /**
     * 云端重算包清单摘要（CR-024 §4.28.4）。
     *
     * <p>使用与该任务下发车端一致的包清单投影计算；规范化输入不含 per-VIN 的 sourceVersion，
     * 因此与发布期冻结值同源同值。重算失败不得阻断安装：返回 null 时域服务回退到冻结值。</p>
     */
    private SnapshotDigest recomputeManifestDigest(VehicleTask vehicleTask, Task task) {
        try {
            String digest = packageManifestDigestService.digest(
                    vehicleTaskPackageService.project(vehicleTask, task, null),
                    installPlanVersionOf(vehicleTask), task.getId().getValue());
            return SnapshotDigest.of(digest, PackageManifestDigestService.ALGORITHM);
        } catch (RuntimeException e) {
            log.warn("车辆任务[{}]包清单摘要重算失败，回退到冻结摘要校验",
                    vehicleTask.getId().getValue(), e);
            return null;
        }
    }

    /**
     * 安装计划版本：与发布冻结、TaskCheck 下发同口径。
     */
    private static String installPlanVersionOf(VehicleTask vehicleTask) {
        return "task-" + vehicleTask.getTaskId() + "-r" + vehicleTask.getTaskRevision().getValue();
    }

    /**
     * 最终结果收口（CR-012 §5.7）。
     *
     * <p>事件连续且结果合法时，原子收口 Execution、保存 ECU 结果并按策略推进 VehicleTask。
     * Execution 收口不等于 VehicleTask 必然终态。
     *
     * @param cmd 收口命令
     * @return 收口结果
     */
    @Transactional
    public ExecutionFinalizeResult finalizeExecution(ExecutionFinalizeCmd cmd) {
        log.info("执行[{}]最终结果收口，最终状态[{}]", cmd.getExecutionId(), cmd.getFinalStatus());

        Execution execution = executionRepository.getById(ExecutionId.of(cmd.getExecutionId()))
                .orElseThrow(() -> new ExecutionStateException("执行[" + cmd.getExecutionId() + "]不存在"));

        // IOV-OTA-DSN-CR-025 §4.29.8：车辆任务被重置后，旧执行已 CANCELED（终态），
        // 其迟到最终结果一律拒绝，不得二次收口、不得推进新车辆任务
        if (execution.isTerminal()) {
            log.warn("执行[{}]已为终态[{}]，拒绝迟到最终结果（车辆任务重置后的迟到数据）",
                    cmd.getExecutionId(), execution.getStatus());
            return ExecutionFinalizeResult.builder()
                    .resultAccepted(false)
                    .executionStatus(execution.getStatus().getValue())
                    .reason("LATE_FINAL_RESULT")
                    .build();
        }

        // 设置最终序号
        if (cmd.getFinalSequenceNo() != null) {
            execution.defineFinalSequenceNo(cmd.getFinalSequenceNo());
        }

        // 水位未达最终序号时，返回缺失范围
        if (!execution.isWatermarkReached()) {
            return ExecutionFinalizeResult.builder()
                    .resultAccepted(false)
                    .missingSequenceRanges(execution.getSequenceWatermark().missingRanges()
                            .stream().map(r -> new long[]{r[0], r[1]}).toList())
                    .build();
        }

        ExecutionStatus finalStatus = ExecutionStatus.valOf(cmd.getFinalStatus());
        execution.finalize(finalStatus);
        // IOV-OTA-DSN-CR-026 §6.3/§7.2：收口时补齐车端起止时间（未知保持 null）
        execution.completeTiming(cmd.getStartedAt(), cmd.getCompletedAt(), java.time.Instant.now());

        // 保存 ECU 结果（CR-019：ECU 聚合行 + per-Target/Slot 软件单元子表）
        if (cmd.getEcuResults() != null) {
            for (EcuResultCmd ecu : cmd.getEcuResults()) {
                ExecutionEcuResultPo ecuPo = ExecutionEcuResultPo.builder()
                        .executionId(cmd.getExecutionId())
                        .ecuId(ecu.getEcuId())
                        .softwareModel(ecu.getSoftwareModel())
                        .targetSoftwareVersion(ecu.getTargetSoftwareVersion())
                        .actualSoftwareVersion(ecu.getActualSoftwareVersion())
                        .result(ecu.getResult())
                        .failReason(ecu.getFailReason())
                        .build();
                executionEcuResultMapper.insert(ecuPo);
                if (ecu.getSoftwareUnitResults() != null) {
                    for (SoftwareUnitResultCmd unit : ecu.getSoftwareUnitResults()) {
                        ExecutionEcuSoftwareUnitPo unitPo = ExecutionEcuSoftwareUnitPo.builder()
                                .ecuResultId(ecuPo.getId())
                                .executionId(cmd.getExecutionId())
                                .ecuId(ecu.getEcuId())
                                .softwareTargetCode(unit.getSoftwareTargetCode())
                                .sourceVersion(unit.getSourceVersion())
                                .targetVersion(unit.getTargetVersion())
                                .actualVersion(unit.getActualVersion())
                                .slot(unit.getSlot())
                                .active(unit.getActive())
                                .result(unit.getResult())
                                .failureStage(unit.getFailureStage())
                                .rollbackResult(unit.getRollbackResult())
                                .packageId(unit.getPackageId())
                                .build();
                        executionEcuSoftwareUnitMapper.insert(unitPo);
                    }
                }
            }
        }

        executionRepository.save(execution);

        // 按策略推进 VehicleTask
        VehicleTask vehicleTask = vehicleTaskRepository.getById(execution.getVehicleTaskId()).orElse(null);
        String vehicleTaskStatus = null;
        if (vehicleTask != null) {
            applyFinalResultToVehicleTask(vehicleTask, finalStatus);
            vehicleTaskRepository.save(vehicleTask);
            vehicleTaskStatus = vehicleTask.getStatus().getValue();
        }

        outboxRepository.append("EXECUTION", String.valueOf(cmd.getExecutionId()),
                "ExecutionFinalized", finalStatus.getValue());

        return ExecutionFinalizeResult.builder()
                .resultAccepted(true)
                .executionStatus(finalStatus.getValue())
                .vehicleTaskStatus(vehicleTaskStatus)
                .build();
    }

    /**
     * 根据 Execution 最终状态推进 VehicleTask。
     *
     * <p>IOV-OTA-DSN-CR-025 §4.29.8：车辆任务已被重置/取代或已取消时不得推进其状态，
     * 否则会抛 {@code VehicleTaskStateException} 并阻断迟到结果处理；此处按“仅记录、不推进”处理。</p>
     */
    private void applyFinalResultToVehicleTask(VehicleTask vehicleTask, ExecutionStatus finalStatus) {
        if (vehicleTask.getStatus() == VehicleTaskStatus.SUPERSEDED
                || vehicleTask.getStatus() == VehicleTaskStatus.CANCELED) {
            log.warn("车辆任务[{}]状态为[{}]，跳过最终结果状态推进（重置后被取代/取消）",
                    vehicleTask.getId().getValue(), vehicleTask.getStatus());
            return;
        }
        switch (finalStatus) {
            case SUCCEEDED -> vehicleTask.onExecutionSucceeded();
            case FAILED -> vehicleTask.onExecutionFailed();
            case ROLLED_BACK -> vehicleTask.onExecutionRolledBack(true);
            default -> {
                // CANCELED / TIMED_OUT 等，清除活动执行
                if (vehicleTask.hasActiveExecution()) {
                    vehicleTask.onExecutionFailed();
                }
            }
        }
    }

    private Long generateId() {
        return idSeed.incrementAndGet();
    }
}
