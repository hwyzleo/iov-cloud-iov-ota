package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleTaskStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.VehicleTaskCreatedEvent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.VehicleTaskEvent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.VehicleTaskStatusChangedEvent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 车辆任务聚合根（CR-012 §2.2）
 *
 * <p>VehicleTask 是单 VIN 长期任务，管理任务快照、授权、下载准备、车辆级状态和重试/回滚等待。
 * 下载准备状态归 VehicleTask；安装过程事件归 Execution，两个通道不得重复推进同一状态字段。
 *
 * <p>状态机：
 * <pre>
 * CREATED -> VISIBLE -> CONSENT_PENDING -> DOWNLOAD_PENDING -> READY_TO_INSTALL
 * READY_TO_INSTALL -> EXECUTING -> SUCCEEDED
 * EXECUTING -> RETRY_PENDING -> READY_TO_INSTALL
 * EXECUTING -> ROLLBACK_PENDING -> EXECUTING/ROLLED_BACK
 * 任意安全点 -> PAUSED/CANCELED/SUPERSEDED
 * </pre>
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
public class VehicleTask {

    private final VehicleTaskId id;
    private final Long taskId;
    private final String vin;

    /** 任务版本号，实质性变化必须升级 */
    @Setter private TaskRevision taskRevision;
    /** 快照摘要，用于检测实质性变化 */
    @Setter private SnapshotDigest snapshotDigest;

    private VehicleTaskStatus status;
    /** 下载准备状态（独立通道，§5.4） */
    @Setter private DownloadReadyState downloadReadyState;
    /** 授权状态（accepted 与 effectiveConsentStatus 分离，§5.3） */
    @Setter private ConsentState consentState;

    /** 时间快照（发布时冻结） */
    private final Instant releaseAt;
    private final Instant startTime;
    private final Instant endTime;

    /** 取代关系 */
    @Setter private VehicleTaskId supersededBy;
    /** 本地任务处置意图（车端可延期/拒绝） */
    @Setter private String localDisposition;
    /** 包缓存处置意图 */
    @Setter private String packageCacheAction;

    /** 当前活动执行 */
    @Setter private ExecutionId activeExecutionId;
    /** 最近一次尝试序号 */
    @Setter private int lastAttemptNo;

    /** 暂停前状态（恢复用） */
    @Setter private VehicleTaskStatus stateBeforePause;

    private final List<VehicleTaskEvent> pendingEvents = new ArrayList<>();

    public static VehicleTask create(VehicleTaskId id, Long taskId, String vin,
                                     TaskRevision taskRevision, SnapshotDigest snapshotDigest,
                                     Instant releaseAt, Instant startTime, Instant endTime) {
        VehicleTask vt = new VehicleTask(id, taskId, vin, releaseAt, startTime, endTime);
        vt.taskRevision = taskRevision;
        vt.snapshotDigest = snapshotDigest;
        vt.status = VehicleTaskStatus.CREATED;
        vt.downloadReadyState = DownloadReadyState.NOT_STARTED;
        vt.consentState = ConsentState.NOT_REQUIRED;
        vt.lastAttemptNo = 0;
        vt.pendingEvents.add(new VehicleTaskCreatedEvent(id, taskId, vin, taskRevision.getValue()));
        log.info("车辆任务[{}]已创建，任务[{}]，车辆[{}]，版本[{}]", id.getValue(), taskId, vin, taskRevision.getValue());
        return vt;
    }

    private VehicleTask(VehicleTaskId id, Long taskId, String vin, Instant releaseAt, Instant startTime, Instant endTime) {
        this.id = id;
        this.taskId = taskId;
        this.vin = vin;
        this.releaseAt = releaseAt;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static VehicleTask fromPo(VehicleTaskId id, Long taskId, String vin,
                                     Instant releaseAt, Instant startTime, Instant endTime) {
        return new VehicleTask(id, taskId, vin, releaseAt, startTime, endTime);
    }

    /**
     * 从持久化重建 VehicleTask（不触发事件）。
     */
    public static VehicleTask reconstitute(VehicleTaskId id, Long taskId, String vin,
                                           TaskRevision taskRevision, SnapshotDigest snapshotDigest,
                                           VehicleTaskStatus status, DownloadReadyState downloadReadyState,
                                           ConsentState consentState, Instant releaseAt, Instant startTime,
                                           Instant endTime, VehicleTaskId supersededBy, String localDisposition,
                                           String packageCacheAction, ExecutionId activeExecutionId,
                                           int lastAttemptNo, VehicleTaskStatus stateBeforePause) {
        VehicleTask vt = new VehicleTask(id, taskId, vin, releaseAt, startTime, endTime);
        vt.taskRevision = taskRevision;
        vt.snapshotDigest = snapshotDigest;
        vt.status = status;
        vt.downloadReadyState = downloadReadyState;
        vt.consentState = consentState;
        vt.supersededBy = supersededBy;
        vt.localDisposition = localDisposition;
        vt.packageCacheAction = packageCacheAction;
        vt.activeExecutionId = activeExecutionId;
        vt.lastAttemptNo = lastAttemptNo;
        vt.stateBeforePause = stateBeforePause;
        return vt;
    }

    // ==================== 状态转换 ====================

    /**
     * 标记为可见（CREATED -> VISIBLE）。
     * 前置：releaseAt <= now。
     */
    public void markVisible(Instant now) {
        validateState(VehicleTaskStatus.CREATED, "只能在已创建状态下标记可见");
        if (releaseAt != null && now.isBefore(releaseAt)) {
            throw new VehicleTaskStateException("尚未到达发布时间，不可标记可见");
        }
        transitionTo(VehicleTaskStatus.VISIBLE);
        log.info("车辆任务[{}]已标记可见", id.getValue());
    }

    /**
     * 进入待授权状态（VISIBLE -> CONSENT_PENDING）。
     */
    public void enterConsentPending() {
        validateState(VehicleTaskStatus.VISIBLE, "只能在已可见状态下进入待授权");
        this.consentState = ConsentState.PENDING;
        transitionTo(VehicleTaskStatus.CONSENT_PENDING);
        log.info("车辆任务[{}]进入待授权", id.getValue());
    }

    /**
     * 授予授权。
     * 若需要下载则进入 DOWNLOAD_PENDING，否则直接 READY_TO_INSTALL。
     */
    public void grantConsent(boolean downloadRequired) {
        if (this.status != VehicleTaskStatus.CONSENT_PENDING && this.status != VehicleTaskStatus.DOWNLOAD_PENDING) {
            throw new VehicleTaskStateException("只能在待授权或待下载状态下授予授权");
        }
        this.consentState = ConsentState.GRANTED;
        if (downloadRequired && this.status == VehicleTaskStatus.CONSENT_PENDING) {
            transitionTo(VehicleTaskStatus.DOWNLOAD_PENDING);
        } else if (!downloadRequired) {
            this.downloadReadyState = DownloadReadyState.VERIFIED;
            transitionTo(VehicleTaskStatus.READY_TO_INSTALL);
        }
        log.info("车辆任务[{}]授权已授予，当前状态[{}]", id.getValue(), status);
    }

    /**
     * 拒绝授权。
     */
    public void denyConsent() {
        this.consentState = ConsentState.DENIED;
        log.info("车辆任务[{}]授权被拒绝", id.getValue());
    }

    /**
     * 撤回授权。不可逆安装阶段仅记录控制意图，不直接中断。
     */
    public void revokeConsent() {
        this.consentState = ConsentState.REVOKED;
        log.info("车辆任务[{}]授权已撤回", id.getValue());
    }

    /**
     * 开始下载（DOWNLOAD_PENDING 状态下，downloadReadyState -> IN_PROGRESS）。
     */
    public void startDownload() {
        validateState(VehicleTaskStatus.DOWNLOAD_PENDING, "只能在待下载状态下开始下载");
        this.downloadReadyState = DownloadReadyState.IN_PROGRESS;
        log.info("车辆任务[{}]开始下载", id.getValue());
    }

    /**
     * 标记下载就绪（DOWNLOAD_PENDING -> READY_TO_INSTALL）。
     * 前置：全部必需包校验成功。
     */
    public void markDownloadReady() {
        validateState(VehicleTaskStatus.DOWNLOAD_PENDING, "只能在待下载状态下标记下载就绪");
        this.downloadReadyState = DownloadReadyState.VERIFIED;
        transitionTo(VehicleTaskStatus.READY_TO_INSTALL);
        log.info("车辆任务[{}]下载就绪", id.getValue());
    }

    /**
     * 标记下载失败。
     */
    public void markDownloadFailed() {
        this.downloadReadyState = DownloadReadyState.FAILED;
        log.info("车辆任务[{}]下载失败", id.getValue());
    }

    /**
     * 绑定活动执行（READY_TO_INSTALL -> EXECUTING）。
     * 设置 activeExecutionId 和 lastAttemptNo。
     */
    public void attachExecution(ExecutionId executionId, int attemptNo) {
        validateState(VehicleTaskStatus.READY_TO_INSTALL, "只能在就绪可安装状态下绑定执行");
        if (this.activeExecutionId != null) {
            throw new VehicleTaskStateException("已存在活动执行，不可重复绑定");
        }
        this.activeExecutionId = executionId;
        this.lastAttemptNo = attemptNo;
        transitionTo(VehicleTaskStatus.EXECUTING);
        log.info("车辆任务[{}]绑定执行[{}]，尝试序号[{}]", id.getValue(), executionId.getValue(), attemptNo);
    }

    /**
     * 执行成功（EXECUTING -> SUCCEEDED），清除活动执行。
     */
    public void onExecutionSucceeded() {
        validateState(VehicleTaskStatus.EXECUTING, "只能在执行中状态下处理执行成功");
        this.activeExecutionId = null;
        transitionTo(VehicleTaskStatus.SUCCEEDED);
        log.info("车辆任务[{}]执行成功", id.getValue());
    }

    /**
     * 执行失败（EXECUTING -> RETRY_PENDING），清除活动执行。
     */
    public void onExecutionFailed() {
        if (this.status != VehicleTaskStatus.EXECUTING && this.status != VehicleTaskStatus.ROLLBACK_PENDING) {
            throw new VehicleTaskStateException("只能在执行中或待回滚状态下处理执行失败");
        }
        this.activeExecutionId = null;
        transitionTo(VehicleTaskStatus.RETRY_PENDING);
        log.info("车辆任务[{}]执行失败，进入待重试", id.getValue());
    }

    /**
     * 回滚完成（EXECUTING/ROLLBACK_PENDING -> ROLLED_BACK 或 RETRY_PENDING）。
     *
     * @param canRetry 是否可重试
     */
    public void onExecutionRolledBack(boolean canRetry) {
        this.activeExecutionId = null;
        if (canRetry) {
            transitionTo(VehicleTaskStatus.RETRY_PENDING);
            log.info("车辆任务[{}]回滚完成，进入待重试", id.getValue());
        } else {
            transitionTo(VehicleTaskStatus.ROLLED_BACK);
            log.info("车辆任务[{}]回滚完成，不可重试", id.getValue());
        }
    }

    /**
     * 重试就绪（RETRY_PENDING -> READY_TO_INSTALL）。
     */
    public void retryReady() {
        validateState(VehicleTaskStatus.RETRY_PENDING, "只能在待重试状态下标记重试就绪");
        transitionTo(VehicleTaskStatus.READY_TO_INSTALL);
        log.info("车辆任务[{}]重试就绪", id.getValue());
    }

    /**
     * 开始回滚（EXECUTING -> ROLLBACK_PENDING）。
     */
    public void startRollback() {
        validateState(VehicleTaskStatus.EXECUTING, "只能在执行中状态下开始回滚");
        transitionTo(VehicleTaskStatus.ROLLBACK_PENDING);
        log.info("车辆任务[{}]开始回滚", id.getValue());
    }

    /**
     * 暂停（任意安全点 -> PAUSED）。
     */
    public void pause() {
        if (this.status == VehicleTaskStatus.SUCCEEDED || this.status == VehicleTaskStatus.ROLLED_BACK
                || this.status == VehicleTaskStatus.CANCELED || this.status == VehicleTaskStatus.SUPERSEDED
                || this.status == VehicleTaskStatus.PAUSED) {
            throw new VehicleTaskStateException("当前状态[" + status + "]不可暂停");
        }
        this.stateBeforePause = this.status;
        transitionTo(VehicleTaskStatus.PAUSED);
        log.info("车辆任务[{}]已暂停，暂停前状态[{}]", id.getValue(), stateBeforePause);
    }

    /**
     * 恢复（PAUSED -> 暂停前状态）。
     */
    public void resume() {
        validateState(VehicleTaskStatus.PAUSED, "只能在已暂停状态下恢复");
        if (this.stateBeforePause != null) {
            this.status = this.stateBeforePause;
            this.stateBeforePause = null;
        } else {
            this.status = VehicleTaskStatus.READY_TO_INSTALL;
        }
        log.info("车辆任务[{}]已恢复，当前状态[{}]", id.getValue(), status);
    }

    /**
     * 取消（任意安全点 -> CANCELED）。
     */
    public void cancel() {
        if (this.status == VehicleTaskStatus.SUCCEEDED || this.status == VehicleTaskStatus.CANCELED
                || this.status == VehicleTaskStatus.SUPERSEDED) {
            throw new VehicleTaskStateException("当前状态[" + status + "]不可取消");
        }
        this.activeExecutionId = null;
        transitionTo(VehicleTaskStatus.CANCELED);
        log.info("车辆任务[{}]已取消", id.getValue());
    }

    /**
     * 取代（任意安全点 -> SUPERSEDED）。
     *
     * @param supersededBy 取代者车辆任务ID
     */
    public void supersede(VehicleTaskId supersededBy) {
        if (this.status == VehicleTaskStatus.SUCCEEDED || this.status == VehicleTaskStatus.SUPERSEDED) {
            throw new VehicleTaskStateException("当前状态[" + status + "]不可取代");
        }
        this.supersededBy = supersededBy;
        this.activeExecutionId = null;
        transitionTo(VehicleTaskStatus.SUPERSEDED);
        log.info("车辆任务[{}]已被[{}]取代", id.getValue(), supersededBy.getValue());
    }

    /**
     * 升级任务版本（实质性变化）。
     *
     * @param newRevision 新版本号
     * @param newDigest   新快照摘要
     */
    public void upgradeRevision(TaskRevision newRevision, SnapshotDigest newDigest) {
        if (newRevision == null || !newRevision.isUpgradeFrom(this.taskRevision)) {
            throw new VehicleTaskStateException("新版本号必须大于当前版本号");
        }
        this.taskRevision = newRevision;
        this.snapshotDigest = newDigest;
        log.info("车辆任务[{}]版本升级至[{}]", id.getValue(), newRevision.getValue());
    }

    // ==================== 查询 ====================

    /**
     * 是否处于可安装就绪状态族。
     */
    public boolean isInReadyState() {
        return this.status == VehicleTaskStatus.READY_TO_INSTALL
                || this.status == VehicleTaskStatus.RETRY_PENDING;
    }

    /**
     * 是否有活动执行。
     */
    public boolean hasActiveExecution() {
        return this.activeExecutionId != null;
    }

    /**
     * 是否处于终态。
     */
    public boolean isTerminal() {
        return this.status == VehicleTaskStatus.SUCCEEDED
                || this.status == VehicleTaskStatus.ROLLED_BACK
                || this.status == VehicleTaskStatus.CANCELED
                || this.status == VehicleTaskStatus.SUPERSEDED;
    }

    public List<VehicleTaskEvent> getPendingEvents() {
        return new ArrayList<>(pendingEvents);
    }

    public void clearPendingEvents() {
        pendingEvents.clear();
    }

    // ==================== 内部方法 ====================

    private void transitionTo(VehicleTaskStatus newStatus) {
        VehicleTaskStatus previous = this.status;
        this.status = newStatus;
        pendingEvents.add(new VehicleTaskStatusChangedEvent(this.id, previous, newStatus));
    }

    private void validateState(VehicleTaskStatus expected, String message) {
        if (this.status != expected) {
            throw new VehicleTaskStateException(message + "，当前状态[" + status + "]");
        }
    }
}
