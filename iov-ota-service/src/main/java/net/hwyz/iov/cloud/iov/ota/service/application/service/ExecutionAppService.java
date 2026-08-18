package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionFinalizeCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.EcuResultCmd;
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
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEcuResultPo;
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

    /** 简易 ID 生成器（TODO: 替换为雪花算法或 DB 序列） */
    private final AtomicLong idSeed = new AtomicLong(System.currentTimeMillis() / 1000);

    /**
     * 申请安装许可，创建 Execution（CR-012 §5.5）。
     *
     * @param cmd 创建命令
     * @return 创建结果（含许可令牌）
     */
    @Transactional
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

        Execution execution = installPermitService.requestInstall(executionId, task, vehicleTask, request, Instant.now());

        executionRepository.save(execution);
        vehicleTaskRepository.save(vehicleTask);

        outboxRepository.append("EXECUTION", String.valueOf(executionId.getValue()),
                "ExecutionCreated", "{}");

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

        // 保存 ECU 结果
        if (cmd.getEcuResults() != null) {
            for (EcuResultCmd ecu : cmd.getEcuResults()) {
                ExecutionEcuResultPo ecuPo = ExecutionEcuResultPo.builder()
                        .executionId(cmd.getExecutionId())
                        .ecuId(ecu.getEcuId())
                        .targetSoftwareVersion(ecu.getTargetSoftwareVersion())
                        .actualSoftwareVersion(ecu.getActualSoftwareVersion())
                        .result(ecu.getResult())
                        .failReason(ecu.getFailReason())
                        .build();
                executionEcuResultMapper.insert(ecuPo);
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
     */
    private void applyFinalResultToVehicleTask(VehicleTask vehicleTask, ExecutionStatus finalStatus) {
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
