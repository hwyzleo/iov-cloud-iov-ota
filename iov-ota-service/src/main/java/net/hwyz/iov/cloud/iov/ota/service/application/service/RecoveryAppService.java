package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionDisposition;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.RecoveryQueryCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.RecoveryResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionControlPo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 恢复应用服务（CR-012 §5.8、US-083）
 *
 * <p>VehicleTask/Execution 对账、补传和恢复策略。
 * 恢复查询按 VEHICLE_TASK 或 EXECUTION 返回权威状态、连续水位、缺失范围和待处理控制。
 * 云端 checkpoint 仅用于对账，不作为 ECU 刷写恢复位置指令。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryAppService {

    private final VehicleTaskRepository vehicleTaskRepository;
    private final ExecutionRepository executionRepository;
    private final ExecutionControlMapper executionControlMapper;

    /**
     * 恢复查询。
     *
     * @param cmd 查询命令
     * @return 恢复结果
     */
    public RecoveryResult query(RecoveryQueryCmd cmd) {
        log.info("车辆[{}]恢复查询，范围[{}]", cmd.getVin(), cmd.getScope());

        if ("EXECUTION".equals(cmd.getScope()) && cmd.getExecutionId() != null) {
            return queryByExecution(cmd.getExecutionId());
        }
        if (cmd.getVehicleTaskId() != null) {
            return queryByVehicleTask(cmd.getVehicleTaskId());
        }
        return RecoveryResult.builder()
                .disposition(ExecutionDisposition.MANUAL_RECOVERY_REQUIRED.getValue())
                .build();
    }

    private RecoveryResult queryByVehicleTask(Long vehicleTaskId) {
        VehicleTask vt = vehicleTaskRepository.getById(VehicleTaskId.of(vehicleTaskId)).orElse(null);
        if (vt == null) {
            return RecoveryResult.builder()
                    .disposition(ExecutionDisposition.MANUAL_RECOVERY_REQUIRED.getValue())
                    .build();
        }

        RecoveryResult.RecoveryResultBuilder builder = RecoveryResult.builder()
                .vehicleTaskId(vehicleTaskId)
                .vehicleTaskStatus(vt.getStatus() != null ? vt.getStatus().getValue() : null)
                .disposition(ExecutionDisposition.CONSISTENT.getValue());

        // 查询活动执行
        Optional<Execution> activeExec = executionRepository.findActiveByVehicleTaskId(VehicleTaskId.of(vehicleTaskId));
        if (activeExec.isPresent()) {
            Execution ex = activeExec.get();
            builder.executionId(ex.getId().getValue())
                    .executionStatus(ex.getStatus().getValue())
                    .attemptNo(ex.getAttemptNo())
                    .acceptedSequenceNo(ex.getSequenceWatermark().getAcceptedSequenceNo())
                    .finalSequenceNo(ex.getFinalSequenceNo())
                    .missingSequenceRanges(ex.getSequenceWatermark().missingRanges()
                            .stream().map(r -> new long[]{r[0], r[1]}).toList())
                    .validUntil(ex.getValidUntil());

            // 待处理控制
            ExecutionControlPo latestControl = executionControlMapper.selectLatestByExecutionId(ex.getId().getValue());
            if (latestControl != null) {
                builder.pendingControlRevision(latestControl.getControlRevision())
                        .pendingControlAction(latestControl.getAction());
            }

            // 判定恢复动作
            if (!ex.getSequenceWatermark().missingRanges().isEmpty()) {
                builder.recoveryAction("RESYNC_EVENTS");
            } else if (ex.isActive()) {
                builder.recoveryAction("CONTINUE");
            }
        }

        return builder.build();
    }

    private RecoveryResult queryByExecution(Long executionId) {
        Execution ex = executionRepository.getById(ExecutionId.of(executionId)).orElse(null);
        if (ex == null) {
            return RecoveryResult.builder()
                    .disposition(ExecutionDisposition.MANUAL_RECOVERY_REQUIRED.getValue())
                    .build();
        }

        List<long[]> missingRanges = ex.getSequenceWatermark().missingRanges()
                .stream().map(r -> new long[]{r[0], r[1]}).toList();

        ExecutionControlPo latestControl = executionControlMapper.selectLatestByExecutionId(executionId);

        String recoveryAction = null;
        if (!missingRanges.isEmpty()) {
            recoveryAction = "RESYNC_EVENTS";
        } else if (ex.isActive()) {
            recoveryAction = "CONTINUE";
        } else {
            recoveryAction = "CLOSED";
        }

        return RecoveryResult.builder()
                .disposition(ExecutionDisposition.CONSISTENT.getValue())
                .vehicleTaskId(ex.getVehicleTaskId().getValue())
                .executionId(executionId)
                .executionStatus(ex.getStatus().getValue())
                .attemptNo(ex.getAttemptNo())
                .acceptedSequenceNo(ex.getSequenceWatermark().getAcceptedSequenceNo())
                .finalSequenceNo(ex.getFinalSequenceNo())
                .missingSequenceRanges(missingRanges)
                .pendingControlRevision(latestControl != null ? latestControl.getControlRevision() : null)
                .pendingControlAction(latestControl != null ? latestControl.getAction() : null)
                .recoveryAction(recoveryAction)
                .validUntil(ex.getValidUntil())
                .build();
    }
}
