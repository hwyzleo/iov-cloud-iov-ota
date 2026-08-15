package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota.FotaEnvelopeFactory;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota.FotaOutboundEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota.FotaOutboxAppender;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ControlCommandCmd;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionControlPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vehicle.fota.v1.Types.ApplyMode;
import vehicle.fota.v1.Types.ControlAction;
import vehicle.fota.v1.Types.ControlCommand;
import vehicle.fota.v1.Types.ControlScope;

import java.time.Instant;
import java.util.UUID;

/**
 * 云端控制命令应用服务（CR-014 §6.3：下行 ControlCommand EVENT）
 *
 * <p>云端控制指令按 controlRevision 单调递增写入领域状态，并以 vehicle.fota.v1.Types.ControlCommand
 * 为 payload 构造 EVENT Envelope 冻结 bytes 入 Kafka Outbox 异步发布。
 * 车端按 controlId + controlRevision 幂等处理并回报 RECEIVED/DEFERRED/APPLIED/REJECTED；
 * OTA 只有在业务回执到达后才能推进控制状态（Kafka 生产成功不推进为 APPLIED）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaControlCommandAppService {

    private final ExecutionRepository executionRepository;
    private final ExecutionControlMapper executionControlMapper;
    private final FotaEnvelopeFactory fotaEnvelopeFactory;
    private final FotaOutboxAppender fotaOutboxAppender;
    private final KafkaMessagingMetricsService metrics;

    /**
     * 下发云端控制命令。
     *
     * @param cmd 控制命令
     * @return controlId
     */
    @Transactional
    public String issueControl(ControlCommandCmd cmd) {
        log.info("云端下发控制命令：执行[{}] 动作[{}] 模式[{}]", cmd.getExecutionId(), cmd.getAction(), cmd.getApplyMode());

        Execution execution = executionRepository.getById(ExecutionId.of(cmd.getExecutionId()))
                .orElseThrow(() -> new ExecutionStateException("执行[" + cmd.getExecutionId() + "]不存在"));
        if (!execution.isActive()) {
            throw new ExecutionStateException("执行[" + cmd.getExecutionId() + "]已收口，不能下发控制");
        }

        // 计算下一 controlRevision（单调递增）
        ExecutionControlPo latest = executionControlMapper.selectLatestByExecutionId(cmd.getExecutionId());
        int nextRevision = latest != null ? latest.getControlRevision() + 1 : 1;

        String controlId = "ctl-" + UUID.randomUUID().toString().replace("-", "");
        long now = Instant.now().toEpochMilli();

        // 写领域状态：控制指令落库（同事务）
        executionControlMapper.insert(ExecutionControlPo.builder()
                .controlId(controlId)
                .executionId(cmd.getExecutionId())
                .controlRevision(nextRevision)
                .action(cmd.getAction())
                .scope(cmd.getScope())
                .applyMode(cmd.getApplyMode())
                .reason(cmd.getReason())
                .build());

        // 构造 ControlCommand EVENT Envelope 并冻结 bytes 入 Outbox（同事务，由发布器原样生产）
        ControlCommand controlCommand = ControlCommand.newBuilder()
                .setControlId(controlId)
                .setControlRevision(nextRevision)
                .setAction(mapAction(cmd.getAction()))
                .setScope(mapScope(cmd.getScope()))
                .setApplyMode(mapApplyMode(cmd.getApplyMode()))
                .setReasonCode(cmd.getReason() == null ? "" : cmd.getReason())
                .setIssuedAtMs(now)
                .setExpiresAtMs(now + 5 * 60 * 1000L)
                .build();
        FotaOutboundEnvelope envelope = fotaEnvelopeFactory.event(
                cmd.getVin(), "", "", String.valueOf(cmd.getExecutionId()), null, null,
                controlCommand, "EXECUTION", String.valueOf(cmd.getExecutionId()));
        fotaOutboxAppender.append(envelope);

        metrics.increment("ota.control.issued");
        log.info("云端控制命令已下发：controlId[{}] revision[{}] action[{}]", controlId, nextRevision, cmd.getAction());
        return controlId;
    }

    private static ControlAction mapAction(String action) {
        if (action == null) {
            return ControlAction.CONTROL_ACTION_CONTINUE;
        }
        return switch (action) {
            case "PAUSE" -> ControlAction.CONTROL_ACTION_PAUSE;
            case "ABORT" -> ControlAction.CONTROL_ACTION_ABORT;
            case "ROLLBACK" -> ControlAction.CONTROL_ACTION_ROLLBACK;
            case "RESYNC" -> ControlAction.CONTROL_ACTION_RESYNC;
            default -> ControlAction.CONTROL_ACTION_CONTINUE;
        };
    }

    private static ControlScope mapScope(String scope) {
        if (scope == null) {
            return ControlScope.CONTROL_SCOPE_EXECUTION;
        }
        return switch (scope) {
            case "PLAN_NODE" -> ControlScope.CONTROL_SCOPE_PLAN_NODE;
            case "ECU_JOB" -> ControlScope.CONTROL_SCOPE_ECU_JOB;
            default -> ControlScope.CONTROL_SCOPE_EXECUTION;
        };
    }

    private static ApplyMode mapApplyMode(String applyMode) {
        if (applyMode == null) {
            return ApplyMode.APPLY_MODE_AT_SAFE_POINT;
        }
        return switch (applyMode) {
            case "IMMEDIATE" -> ApplyMode.APPLY_MODE_IMMEDIATE_IF_SAFE;
            case "BEFORE_NEXT_NODE" -> ApplyMode.APPLY_MODE_BEFORE_NEXT_NODE;
            default -> ApplyMode.APPLY_MODE_AT_SAFE_POINT;
        };
    }
}
