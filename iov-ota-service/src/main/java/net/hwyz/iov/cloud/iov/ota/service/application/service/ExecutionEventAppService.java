package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ControlAckCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionEventCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SequenceWatermark;
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
     * <p><b>并发与可恢复性（2026-11-15 实车加固）</b>：
     * <ul>
     *   <li>先对 Execution 行加悲观写锁（SELECT ... FOR UPDATE），串行化同执行的事件/收口写入，
     *       避免整行写回把 acceptedSequenceNo 写回小值；</li>
     *   <li>乱序缓冲不属于聚合持久化字段，每次加载从事件表重建（{@code disposition=BUFFERED
     *       且 sequence_no > 水位}），保证 missing_sequence_ranges 可回、已暂存事件可被吸收；</li>
     *   <li>幂等重放（同 eventId）不重复落事件行，但按当前水位重新评估，允许重投把已可接受的
     *       序号真正推进（旧实现固定回原 disposition，导致 seq 永久 BUFFERED）。</li>
     * </ul>
     *
     * @param cmd 事件命令
     * @return 事件结果（含处置、水位、缺失范围、最新控制）
     */
    @Transactional
    public ExecutionEventResult receiveEvent(ExecutionEventCmd cmd) {
        log.info("执行[{}]接收事件[{}]，序号[{}]", cmd.getExecutionId(), cmd.getEventId(), cmd.getSequenceNo());

        Execution execution = executionRepository.getByIdForUpdate(ExecutionId.of(cmd.getExecutionId()))
                .orElseThrow(() -> new ExecutionStateException("执行[" + cmd.getExecutionId() + "]不存在"));
        boolean healed = restoreFromEventFacts(execution, cmd.getExecutionId());

        // IOV-OTA-DSN-CR-025 §4.29.8：车辆任务重置后旧执行已 CANCELED（终态），
        // 其迟到事件一律拒绝，不得推进水位、不得污染重置后的车辆任务
        if (execution.isTerminal()) {
            log.warn("执行[{}]已为终态[{}]，拒绝迟到事件[{}]（seq={}）",
                    cmd.getExecutionId(), execution.getStatus(), cmd.getEventId(), cmd.getSequenceNo());
            return ExecutionEventResult.builder()
                    .disposition("LATE_EVENT")
                    .acceptedSequenceNo(execution.getSequenceWatermark().getAcceptedSequenceNo())
                    .missingSequenceRanges(List.of())
                    .build();
        }

        // 幂等：eventId 已存在时不重复落事件行，但必须按当前水位重新评估
        ExecutionEventPo existing = executionEventMapper.selectByEventId(cmd.getEventId());
        if (existing != null) {
            SequenceWatermark.Disposition replay =
                    execution.receiveEvent(cmd.getSequenceNo());
            if (replay == SequenceWatermark.Disposition.ACCEPTED) {
                existing.setDisposition("ACCEPTED");
                executionEventMapper.updateById(existing);
            }
            if (replay == SequenceWatermark.Disposition.ACCEPTED || healed) {
                maybeRecordInstallStarted(execution, cmd);
                executionRepository.save(execution);
                markBufferedAbsorbed(cmd.getExecutionId(), execution);
            }
            log.info("执行[{}]事件[{}]幂等重放，处置[{}]，水位[{}]",
                    cmd.getExecutionId(), cmd.getEventId(), replay,
                    execution.getSequenceWatermark().getAcceptedSequenceNo());
            return eventResult(responseDisposition(replay), execution, cmd.getExecutionId());
        }

        // 推进水位
        SequenceWatermark.Disposition disposition =
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
                .sourceOccurredAt(cmd.getOccurredAt() == null ? null : new Date(cmd.getOccurredAt().toEpochMilli()))
                .progressPercent(cmd.getProgressPercent())
                .ecuId(cmd.getEcuId())
                .softwareTargetCode(cmd.getSoftwareTargetCode())
                .slot(cmd.getSlot())
                .errorCode(cmd.getErrorCode())
                .failureReason(cmd.getFailureReason())
                .receivedTime(new Date())
                .build();
        executionEventMapper.insert(eventPo);

        maybeRecordInstallStarted(execution, cmd);
        executionRepository.save(execution);
        markBufferedAbsorbed(cmd.getExecutionId(), execution);

        return eventResult(responseDisposition(disposition), execution, cmd.getExecutionId());
    }

    /**
     * 首个权威安装开始事件补写 Execution 开始时间（IOV-OTA-DSN-CR-026 §7.2）。
     *
     * <p>仅当事件为 INSTALL 阶段且 event_status=INSTALL_STARTED（或阶段直接为
     * INSTALL_STARTED）时生效；occurred_at_ms=0 视为未知，不写 1970。
     */
    private void maybeRecordInstallStarted(Execution execution, ExecutionEventCmd cmd) {
        if (execution.getStartTime() != null) {
            return;
        }
        // 优先权威 INSTALL_STARTED；车端（含 Mock 场景）若只发 INSTALL + 进度事件，
        // 首个 INSTALL 事件同样视为安装开始事实（occurred_at_ms 有效才写）。
        boolean installStarted = "INSTALL_STARTED".equalsIgnoreCase(cmd.getEventType())
                || "INSTALL".equalsIgnoreCase(cmd.getEventType())
                || ("INSTALL".equalsIgnoreCase(cmd.getEventStatus()));
        if (!installStarted) {
            return;
        }
        execution.recordInstallStarted(cmd.getOccurredAt(), java.time.Instant.now());
    }

    /**
     * 从事件事实重建水位与乱序缓冲（自愈）。
     *
     * <p>已落库序号构成连续前缀 1..k 时，k 就是权威水位：即使 {@code accepted_sequence_no}
     * 曾被旧实现写回小值（实车 2026-11-15 从 2 回退到 1），下一次加载也会自动纠正；
     * 大于 k 的已收事件进入 buffered，保证 missing_sequence_ranges 可回、且能被吸收。
     */
    private boolean restoreFromEventFacts(Execution execution, Long executionId) {
        List<Long> received = executionEventMapper.selectReceivedSequenceNos(executionId);
        if (received == null || received.isEmpty()) {
            return false;
        }
        long stored = execution.getSequenceWatermark().getAcceptedSequenceNo();
        long derived = contiguousPrefix(received);
        execution.advanceWatermarkTo(derived);
        long watermark = execution.getSequenceWatermark().getAcceptedSequenceNo();
        execution.restoreBufferedSequenceNos(received.stream()
                .filter(seq -> seq != null && seq > watermark)
                .toList());
        return watermark > stored;
    }

    /**
     * 已落库序号集合的连续前缀长度（1..k 全部存在时的 k；不存在 1 时为 0）。
     */
    private static long contiguousPrefix(List<Long> receivedSequenceNos) {
        long watermark = 0L;
        for (Long sequenceNo : receivedSequenceNos) {
            if (sequenceNo == null) {
                continue;
            }
            if (sequenceNo == watermark + 1) {
                watermark++;
            } else if (sequenceNo > watermark + 1) {
                break;
            }
        }
        return watermark;
    }

    /**
     * 对外应答处置：序号已在水位内（DUPLICATE）按 ACCEPTED 应答。
     *
     * <p>契约上 DUPLICATE 表示「该序号已被接受」，车端（CCU-FOTA）仅在 ACCEPTED 时标记
     * outbox ACKED、否则一直补发；若对已吸收序号回 DUPLICATE 会形成补发死循环。
     * 事件行仍以 DUPLICATE 落库保留审计事实。
     */
    private static String responseDisposition(SequenceWatermark.Disposition disposition) {
        return disposition == SequenceWatermark.Disposition.DUPLICATE
                ? "ACCEPTED" : disposition.name();
    }

    /**
     * 水位推进后把已吸收的 BUFFERED 行置为 ACCEPTED，保持事件表与水位一致。
     */
    private void markBufferedAbsorbed(Long executionId, Execution execution) {
        long accepted = execution.getSequenceWatermark().getAcceptedSequenceNo();
        if (accepted > 0) {
            executionEventMapper.markBufferedAbsorbed(executionId, accepted);
        }
    }

    /**
     * 构造事件结果（含最新控制指令）。
     */
    private ExecutionEventResult eventResult(String dispositionStr, Execution execution, Long executionId) {
        ExecutionControlPo latestControl = executionControlMapper.selectLatestByExecutionId(executionId);
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
