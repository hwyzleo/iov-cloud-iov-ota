package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.ExecutionCreatedEvent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.ExecutionEvent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.ExecutionFinalizedEvent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.ExecutionStatusChangedEvent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SequenceWatermark;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 安装执行聚合根（CR-012 §2.3）
 *
 * <p>Execution 是一次安装尝试。同一 VehicleTask 可顺序产生多个 Execution，但同时最多一个活动实例。
 *
 * <p>核心不变量：
 * <ul>
 *   <li>(vehicleTaskId, attemptNo) 唯一；同一 VehicleTask 同时最多一个活动 Execution</li>
 *   <li>创建时冻结 taskRevision、installPlanVersion、packageManifestDigest、conditionSetVersion、
 *       授权凭据、离线和超时策略</li>
 *   <li>validUntil 仅限制进入 INSTALL_STARTED；已开始后不得因许可自然过期直接中断</li>
 *   <li>Execution 收口不等于 VehicleTask 必然终态</li>
 * </ul>
 *
 * <p>状态：PERMITTED / INSTALLING / PAUSED / ROLLING_BACK / SUCCEEDED / FAILED / ROLLED_BACK / CANCELED / TIMED_OUT
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
public class Execution {

    private final ExecutionId id;
    private final VehicleTaskId vehicleTaskId;
    private final int attemptNo;

    private ExecutionStatus status;

    /** 创建时冻结的策略版本 */
    private final TaskRevision taskRevision;
    private final String installPlanVersion;
    private final SnapshotDigest packageManifestDigest;
    private final String conditionSetVersion;

    /** 安装许可令牌 */
    private final PermitToken permitToken;
    /** 离线策略（JSON） */
    @Setter private String offlinePolicy;
    /** 超时策略（JSON） */
    @Setter private String timeoutPolicy;
    /** 控制策略（JSON） */
    @Setter private String controlPolicy;

    /** 许可有效期，仅限制进入 INSTALL_STARTED */
    private final Instant validUntil;

    /** 事件连续水位（§5.6） */
    private final SequenceWatermark sequenceWatermark;
    /** 最终序号（收口时校验） */
    @Setter private long finalSequenceNo;

    private final List<ExecutionEvent> pendingEvents = new ArrayList<>();

    /**
     * 创建并许可一次安装执行。
     *
     * @param id                     执行ID
     * @param vehicleTaskId          车辆任务ID
     * @param attemptNo              尝试序号
     * @param taskRevision           冻结的任务版本
     * @param installPlanVersion     冻结的安装计划版本
     * @param packageManifestDigest  冻结的包清单摘要
     * @param conditionSetVersion    冻结的条件集版本
     * @param permitToken            签发的许可令牌
     * @param validUntil             许可有效期
     */
    public static Execution permit(ExecutionId id, VehicleTaskId vehicleTaskId, int attemptNo,
                                   TaskRevision taskRevision, String installPlanVersion,
                                   SnapshotDigest packageManifestDigest, String conditionSetVersion,
                                   PermitToken permitToken, Instant validUntil) {
        Execution ex = new Execution(id, vehicleTaskId, attemptNo, taskRevision, installPlanVersion,
                packageManifestDigest, conditionSetVersion, permitToken, validUntil);
        ex.status = ExecutionStatus.PERMITTED;
        ex.pendingEvents.add(new ExecutionCreatedEvent(id, vehicleTaskId, attemptNo, permitToken.getToken()));
        log.info("执行[{}]已许可，车辆任务[{}]，尝试序号[{}]", id.getValue(), vehicleTaskId.getValue(), attemptNo);
        return ex;
    }

    private Execution(ExecutionId id, VehicleTaskId vehicleTaskId, int attemptNo,
                      TaskRevision taskRevision, String installPlanVersion,
                      SnapshotDigest packageManifestDigest, String conditionSetVersion,
                      PermitToken permitToken, Instant validUntil) {
        this.id = id;
        this.vehicleTaskId = vehicleTaskId;
        this.attemptNo = attemptNo;
        this.taskRevision = taskRevision;
        this.installPlanVersion = installPlanVersion;
        this.packageManifestDigest = packageManifestDigest;
        this.conditionSetVersion = conditionSetVersion;
        this.permitToken = permitToken;
        this.validUntil = validUntil;
        this.sequenceWatermark = new SequenceWatermark();
        this.finalSequenceNo = 0;
    }

    /**
     * 从持久化重建 Execution（不触发事件）。
     */
    public static Execution reconstitute(ExecutionId id, VehicleTaskId vehicleTaskId, int attemptNo,
                                         ExecutionStatus status, TaskRevision taskRevision,
                                         String installPlanVersion, SnapshotDigest packageManifestDigest,
                                         String conditionSetVersion, PermitToken permitToken, Instant validUntil,
                                         long acceptedSequenceNo, long finalSequenceNo,
                                         String offlinePolicy, String timeoutPolicy, String controlPolicy) {
        Execution ex = new Execution(id, vehicleTaskId, attemptNo, taskRevision, installPlanVersion,
                packageManifestDigest, conditionSetVersion, permitToken, validUntil);
        ex.status = status;
        ex.sequenceWatermark.reset(acceptedSequenceNo);
        ex.finalSequenceNo = finalSequenceNo;
        ex.offlinePolicy = offlinePolicy;
        ex.timeoutPolicy = timeoutPolicy;
        ex.controlPolicy = controlPolicy;
        return ex;
    }

    // ==================== 状态转换 ====================

    /**
     * 开始安装（PERMITTED -> INSTALLING）。
     * 前置：许可未过期（validUntil 仅限制进入 INSTALL_STARTED）。
     */
    public void startInstall(Instant now) {
        validateState(ExecutionStatus.PERMITTED, "只能在已许可状态下开始安装");
        if (validUntil != null && !now.isBefore(validUntil)) {
            throw new ExecutionStateException("安装许可已过期，不可开始安装");
        }
        transitionTo(ExecutionStatus.INSTALLING);
        log.info("执行[{}]开始安装", id.getValue());
    }

    /**
     * 暂停（INSTALLING -> PAUSED）。
     */
    public void pause() {
        validateState(ExecutionStatus.INSTALLING, "只能在安装中状态下暂停");
        transitionTo(ExecutionStatus.PAUSED);
        log.info("执行[{}]已暂停", id.getValue());
    }

    /**
     * 恢复（PAUSED -> INSTALLING）。
     */
    public void resume() {
        validateState(ExecutionStatus.PAUSED, "只能在已暂停状态下恢复");
        transitionTo(ExecutionStatus.INSTALLING);
        log.info("执行[{}]已恢复", id.getValue());
    }

    /**
     * 开始回滚（INSTALLING/PAUSED -> ROLLING_BACK）。
     */
    public void startRollback() {
        if (this.status != ExecutionStatus.INSTALLING && this.status != ExecutionStatus.PAUSED) {
            throw new ExecutionStateException("只能在安装中或已暂停状态下开始回滚");
        }
        transitionTo(ExecutionStatus.ROLLING_BACK);
        log.info("执行[{}]开始回滚", id.getValue());
    }

    /**
     * 安装成功（INSTALLING -> SUCCEEDED）。
     */
    public void succeed() {
        validateState(ExecutionStatus.INSTALLING, "只能在安装中状态下标记成功");
        transitionTo(ExecutionStatus.SUCCEEDED);
        log.info("执行[{}]安装成功", id.getValue());
    }

    /**
     * 安装失败（INSTALLING/ROLLING_BACK -> FAILED）。
     */
    public void fail() {
        if (this.status != ExecutionStatus.INSTALLING && this.status != ExecutionStatus.ROLLING_BACK
                && this.status != ExecutionStatus.PAUSED) {
            throw new ExecutionStateException("只能在安装中、回滚中或已暂停状态下标记失败");
        }
        transitionTo(ExecutionStatus.FAILED);
        log.info("执行[{}]安装失败", id.getValue());
    }

    /**
     * 回滚完成（ROLLING_BACK -> ROLLED_BACK）。
     */
    public void rolledBack() {
        validateState(ExecutionStatus.ROLLING_BACK, "只能在回滚中状态下标记回滚完成");
        transitionTo(ExecutionStatus.ROLLED_BACK);
        log.info("执行[{}]回滚完成", id.getValue());
    }

    /**
     * 取消（PERMITTED/INSTALLING/PAUSED -> CANCELED）。
     */
    public void cancel() {
        if (this.status == ExecutionStatus.SUCCEEDED || this.status == ExecutionStatus.FAILED
                || this.status == ExecutionStatus.ROLLED_BACK || this.status == ExecutionStatus.CANCELED
                || this.status == ExecutionStatus.TIMED_OUT) {
            throw new ExecutionStateException("当前状态[" + status + "]不可取消");
        }
        transitionTo(ExecutionStatus.CANCELED);
        log.info("执行[{}]已取消", id.getValue());
    }

    /**
     * 超时（INSTALLING/PAUSED -> TIMED_OUT）。
     * 到点判超时，过程回传中不计门禁失败。
     */
    public void timeout() {
        if (this.status != ExecutionStatus.INSTALLING && this.status != ExecutionStatus.PAUSED) {
            throw new ExecutionStateException("只能在安装中或已暂停状态下标记超时");
        }
        transitionTo(ExecutionStatus.TIMED_OUT);
        log.info("执行[{}]已超时", id.getValue());
    }

    // ==================== 事件与水位 ====================

    /**
     * 接收一个安装事件，判定处置并推进水位（§5.6）。
     *
     * <p>事件使用 eventId 与 (executionId,sequenceNo) 双重唯一键。
     * 乱序事件可 BUFFER，但 acceptedSequenceNo 只推进到连续接收水位。
     * 只有连续事件才可推进 Execution 状态；BUFFERED 事件不得提前推进业务状态。
     *
     * @param sequenceNo 事件序号
     * @return ACCEPTED / DUPLICATE / BUFFERED
     */
    public SequenceWatermark.Disposition receiveEvent(long sequenceNo) {
        SequenceWatermark.Disposition disposition = sequenceWatermark.classify(sequenceNo);
        switch (disposition) {
            case ACCEPTED -> sequenceWatermark.tryAdvance(sequenceNo);
            case BUFFERED -> sequenceWatermark.buffer(sequenceNo);
            case DUPLICATE -> {
                // 重复事件，不推进
            }
        }
        log.debug("执行[{}]接收事件序号[{}]，处置[{}]，水位[{}]",
                id.getValue(), sequenceNo, disposition, sequenceWatermark.getAcceptedSequenceNo());
        return disposition;
    }

    /**
     * 设置最终序号（车端上报最终结果时）。
     */
    public void defineFinalSequenceNo(long finalSequenceNo) {
        this.finalSequenceNo = finalSequenceNo;
    }

    /**
     * 事件连续水位是否已达到最终序号（可收口条件之一）。
     */
    public boolean isWatermarkReached() {
        return finalSequenceNo > 0 && sequenceWatermark.getAcceptedSequenceNo() >= finalSequenceNo;
    }

    /**
     * 收口执行（§5.7）。
     * 前置：事件连续水位达到最终序号。
     *
     * @param finalStatus 最终状态
     */
    public void finalize(ExecutionStatus finalStatus) {
        if (!isWatermarkReached()) {
            throw new ExecutionStateException("事件连续水位未达到最终序号，不可收口");
        }
        this.status = finalStatus;
        pendingEvents.add(new ExecutionFinalizedEvent(this.id, finalStatus, sequenceWatermark.getAcceptedSequenceNo()));
        log.info("执行[{}]已收口，最终状态[{}]", id.getValue(), finalStatus);
    }

    // ==================== 查询 ====================

    /**
     * 是否处于活动状态（非终态）。
     */
    public boolean isActive() {
        return this.status != ExecutionStatus.SUCCEEDED
                && this.status != ExecutionStatus.FAILED
                && this.status != ExecutionStatus.ROLLED_BACK
                && this.status != ExecutionStatus.CANCELED
                && this.status != ExecutionStatus.TIMED_OUT;
    }

    /**
     * 是否处于终态。
     */
    public boolean isTerminal() {
        return !isActive();
    }

    public List<ExecutionEvent> getPendingEvents() {
        return new ArrayList<>(pendingEvents);
    }

    public void clearPendingEvents() {
        pendingEvents.clear();
    }

    // ==================== 内部方法 ====================

    private void transitionTo(ExecutionStatus newStatus) {
        ExecutionStatus previous = this.status;
        this.status = newStatus;
        pendingEvents.add(new ExecutionStatusChangedEvent(this.id, previous, newStatus));
    }

    private void validateState(ExecutionStatus expected, String message) {
        if (this.status != expected) {
            throw new ExecutionStateException(message + "，当前状态[" + status + "]");
        }
    }
}
