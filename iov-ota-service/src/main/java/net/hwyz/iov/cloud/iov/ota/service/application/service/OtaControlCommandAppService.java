package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ControlCommandCmd;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionControlPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 云端控制命令应用服务（CR-013 §4：下行 ota.execution.control.issued）
 *
 * <p>云端控制指令按 controlRevision 单调递增写入领域状态，并追加下行命令到 Kafka Outbox 异步发布。
 * 车端按 controlId + controlRevision 幂等处理并回报 RECEIVED/DEFERRED/APPLIED/REJECTED；
 * OTA 只有在业务回执到达后才能推进控制状态（Broker/Kafka 生产成功不推进为 APPLIED）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaControlCommandAppService {

    private final ExecutionRepository executionRepository;
    private final ExecutionControlMapper executionControlMapper;
    private final KafkaOutboxRepository kafkaOutboxRepository;
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

        // 追加下行命令到 Kafka Outbox（同事务，由发布器异步生产 ota.execution.control.issued）
        kafkaOutboxRepository.append(KafkaOutboxPo.builder()
                .aggregateType("EXECUTION")
                .aggregateId(String.valueOf(cmd.getExecutionId()))
                .messageType(OtaMessageType.EXECUTION_CONTROL_ISSUED.getValue())
                .messageKey(String.valueOf(cmd.getExecutionId()))
                .correlationId(null)
                .vin(cmd.getVin())
                .payloadJson("{\"controlId\":\"" + controlId + "\",\"executionId\":" + cmd.getExecutionId()
                        + ",\"controlRevision\":" + nextRevision + ",\"action\":\"" + cmd.getAction()
                        + "\",\"scope\":\"" + (cmd.getScope() == null ? "" : cmd.getScope())
                        + "\",\"applyMode\":\"" + (cmd.getApplyMode() == null ? "SAFE_POINT" : cmd.getApplyMode())
                        + "\",\"reason\":\"" + (cmd.getReason() == null ? "" : cmd.getReason()) + "\"}")
                .build());

        metrics.increment("ota.control.issued");
        log.info("云端控制命令已下发：controlId[{}] revision[{}] action[{}]", controlId, nextRevision, cmd.getAction());
        return controlId;
    }
}
