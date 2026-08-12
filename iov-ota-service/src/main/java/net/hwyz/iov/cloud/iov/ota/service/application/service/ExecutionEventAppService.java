package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ControlAckCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionEventCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlAckMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEventMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionControlAckPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionControlPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEventPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 安装事件应用服务（CR-012 §5.6、US-080）
 *
 * <p>顺序事件、连续水位、缺失范围和控制指令响应。
 * 事件以双重唯一键（eventId + executionId/sequenceNo）处理，乱序事件可 BUFFER，
 * 但只有连续事件才可推进 Execution 状态。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionEventAppService {

    private final ExecutionRepository executionRepository;
    private final ExecutionEventMapper executionEventMapper;
    private final ExecutionControlMapper executionControlMapper;
    private final ExecutionControlAckMapper executionControlAckMapper;

    /**
     * 接收安装事件，推进连续水位（CR-012 §5.6）。
     *
     * @param cmd 事件命令
     * @return 事件结果（含处置、水位、缺失范围、最新控制）
     */
    @Transactional
    public ExecutionEventResult receiveEvent(ExecutionEventCmd cmd) {
        log.info("执行[{}]接收事件[{}]，序号[{}]", cmd.getExecutionId(), cmd.getEventId(), cmd.getSequenceNo());

        Execution execution = executionRepository.getById(ExecutionId.of(cmd.getExecutionId()))
                .orElseThrow(() -> new ExecutionStateException("执行[" + cmd.getExecutionId() + "]不存在"));

        // 幂等：eventId 已存在时返回原处置
        ExecutionEventPo existing = executionEventMapper.selectByEventId(cmd.getEventId());
        if (existing != null) {
            return ExecutionEventResult.builder()
                    .disposition(existing.getDisposition())
                    .acceptedSequenceNo(execution.getSequenceWatermark().getAcceptedSequenceNo())
                    .missingSequenceRanges(execution.getSequenceWatermark().missingRanges()
                            .stream().map(r -> new long[]{r[0], r[1]}).toList())
                    .build();
        }

        // 推进水位
        net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SequenceWatermark.Disposition disposition =
                execution.receiveEvent(cmd.getSequenceNo());

        // 映射处置
        String dispositionStr = switch (disposition) {
            case ACCEPTED -> "ACCEPTED";
            case DUPLICATE -> "DUPLICATE";
            case BUFFERED -> "BUFFERED";
        };

        // 持久化事件（双重唯一键保证幂等）
        ExecutionEventPo eventPo = ExecutionEventPo.builder()
                .eventId(cmd.getEventId())
                .executionId(cmd.getExecutionId())
                .sequenceNo(cmd.getSequenceNo())
                .eventType(cmd.getEventType())
                .eventDigest(cmd.getEventDigest())
                .eventPayload(cmd.getEventPayload())
                .disposition(dispositionStr)
                .receivedTime(new Date())
                .build();
        executionEventMapper.insert(eventPo);

        executionRepository.save(execution);

        // 查询最新有效控制
        ExecutionControlPo latestControl = executionControlMapper.selectLatestByExecutionId(cmd.getExecutionId());

        return ExecutionEventResult.builder()
                .disposition(dispositionStr)
                .acceptedSequenceNo(execution.getSequenceWatermark().getAcceptedSequenceNo())
                .missingSequenceRanges(execution.getSequenceWatermark().missingRanges()
                        .stream().map(r -> new long[]{r[0], r[1]}).toList())
                .latestControlRevision(latestControl != null ? latestControl.getControlRevision() : null)
                .latestControlAction(latestControl != null ? latestControl.getAction() : null)
                .build();
    }

    /**
     * 接收控制回执（CR-012 §5.6）。
     *
     * @param cmd 控制回执命令
     */
    @Transactional
    public void receiveControlAck(ControlAckCmd cmd) {
        log.info("执行[{}]接收控制回执，控制[{}]，状态[{}]",
                cmd.getExecutionId(), cmd.getControlId(), cmd.getAckStatus());

        ExecutionControlAckPo ackPo = ExecutionControlAckPo.builder()
                .controlAckId(cmd.getControlAckId())
                .controlId(cmd.getControlId())
                .executionId(cmd.getExecutionId())
                .ackSequenceNo(cmd.getAckSequenceNo())
                .ackStatus(cmd.getAckStatus())
                .ackPayload(cmd.getAckPayload())
                .ackTime(new Date())
                .build();
        executionControlAckMapper.insert(ackPo);
    }
}
