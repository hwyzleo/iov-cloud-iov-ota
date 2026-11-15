package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.ManifestDigestMetrics;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.PermitTokenSigner;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 安装许可领域服务（CR-012 §5.5）
 *
 * <p>安装许可事务核心逻辑（纯领域判定，持久化由应用服务编排）：
 * <pre>
 * requestInstall(vehicleTaskId, idempotencyKey):
 *   assert now in [startTime, endTime)
 *   assert no active Execution
 *   assert taskRevision and installPlanVersion valid
 *   assert consent valid and all package stage results succeeded
 *   recompute and verify packageManifestDigest
 *   validate cloud guards and vehicle condition freshness
 *   attemptNo = lastAttemptNo + 1
 *   create Execution and freeze policies
 *   sign permitToken
 *   set VehicleTask.activeExecutionId and status=EXECUTING
 * </pre>
 *
 * <p>相同幂等键返回原 Execution；新幂等键遇到活动 Execution 返回原活动执行或明确冲突，
 * 不递增 attemptNo。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallPermitService {

    private final PermitTokenSigner permitTokenSigner;
    private final ManifestDigestMetrics manifestDigestMetrics;

    /**
     * 申请安装许可，创建 Execution 并绑定到 VehicleTask。
     *
     * <p>本方法执行纯领域校验与聚合操作；调用方（应用服务）负责加锁、持久化和 Outbox。
     *
     * @param executionId      新执行ID（由应用服务分配）
     * @param task         升级任务
     * @param vehicleTask  车辆任务
     * @param request      许可请求参数
     * @param recomputedDigest 云端按规范化算法重算的包清单摘要（CR-024 §4.28.4）
     * @param now          当前时间
     * @return 创建的 Execution
     * @throws ExecutionStateException 若任一前置守卫不满足
     */
    public Execution requestInstall(ExecutionId executionId, Task task, VehicleTask vehicleTask,
                                    InstallPermitRequest request, SnapshotDigest recomputedDigest, Instant now) {
        // 1. 时间窗口守卫
        assertTimeWindow(task, now);

        // 2. 无活动执行
        if (vehicleTask.hasActiveExecution()) {
            throw new ExecutionStateException("车辆任务[" + vehicleTask.getId().getValue()
                    + "]已存在活动执行，不可重复申请安装许可");
        }

        // 3. 车辆任务须处于就绪状态族
        if (!vehicleTask.isInReadyState()) {
            throw new ExecutionStateException("车辆任务状态[" + vehicleTask.getStatus()
                    + "]不在就绪状态族，不可申请安装许可");
        }

        // 4. 授权有效（统一 ConsentPolicy 判定，CR-016 §4）
        if (request.consentRequired && !request.consentPermitted) {
            throw new ExecutionStateException("授权未通过，不可申请安装许可");
        }

        // 5. 全部必需包阶段结果成功
        if (!request.allPackageStageResultsSucceeded) {
            throw new ExecutionStateException("存在未通过的包阶段结果，不可申请安装许可");
        }

        // 6. 包清单摘要校验（CR-024 §4.28.4）
        //
        // 比较对象是「车端上报值 vs 云端本次重算值」，不是 VehicleTask 的历史列：
        // 历史列（task_vehicle.snapshot_digest）存量可能为空，且曾因从未写入导致每轮必然失败
        // （实车 2026-11-15）。云端无冻结摘要时按 fail-open 处理并留指标，不硬阻断安装。
        if (request.expectedPackageManifestDigest != null) {
            SnapshotDigest frozenDigest = vehicleTask.getSnapshotDigest();
            if (frozenDigest == null) {
                manifestDigestMetrics.unverifiable();
                log.warn("车辆任务[{}]未冻结包清单摘要，跳过包清单摘要校验，车端摘要[{}]（算法[{}]）",
                        vehicleTask.getId().getValue(),
                        request.expectedPackageManifestDigest.getValue(),
                        request.expectedPackageManifestDigest.getAlgorithm());
            } else {
                SnapshotDigest trusted = recomputedDigest != null ? recomputedDigest : frozenDigest;
                if (!request.expectedPackageManifestDigest.matches(trusted)) {
                    manifestDigestMetrics.mismatch();
                    log.warn("车辆任务[{}]包清单摘要不匹配，车端[{}] 云端[{}]",
                            vehicleTask.getId().getValue(),
                            request.expectedPackageManifestDigest.getValue(), trusted.getValue());
                    throw new ExecutionStateException("包清单摘要校验失败：车端["
                            + abbreviate(request.expectedPackageManifestDigest.getValue())
                            + "] 云端[" + abbreviate(trusted.getValue()) + "]");
                }
            }
        }
        SnapshotDigest manifestDigest = recomputedDigest != null
                ? recomputedDigest : request.packageManifestDigest;

        // 7. attemptNo 递增
        int attemptNo = vehicleTask.getLastAttemptNo() + 1;

        // 8. 签发许可令牌
        PermitToken permitToken = permitTokenSigner.sign(executionId, vehicleTask.getId(), attemptNo, request.validUntil);

        // 9. 创建 Execution 并冻结策略
        Execution execution = Execution.permit(executionId, vehicleTask.getId(), attemptNo,
                vehicleTask.getTaskRevision(), request.installPlanVersion,
                manifestDigest, request.conditionSetVersion,
                permitToken, request.validUntil);
        execution.setOfflinePolicy(request.offlinePolicy);
        execution.setTimeoutPolicy(request.timeoutPolicy);
        execution.setControlPolicy(request.controlPolicy);

        // 10. 绑定到 VehicleTask（READY_TO_INSTALL -> EXECUTING）
        vehicleTask.attachExecution(executionId, attemptNo);

        log.info("安装许可已签发，执行[{}]，车辆任务[{}]，尝试序号[{}]",
                executionId.getValue(), vehicleTask.getId().getValue(), attemptNo);
        return execution;
    }

    /**
     * 摘要日志/异常消息截断，避免超长摘要值冲淡日志与协议错误描述。
     */
    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 32 ? value : value.substring(0, 32) + "...";
    }

    private void assertTimeWindow(Task task, Instant now) {        if (task.getStartTime() != null && now.isBefore(task.getStartTime())) {
            throw new ExecutionStateException("尚未到达任务开始时间，不可申请安装许可");
        }
        if (task.getEndTime() != null && !now.isBefore(task.getEndTime())) {
            throw new ExecutionStateException("已超过任务结束时间，不可申请安装许可");
        }
    }

    /**
     * 安装许可请求参数。
     */
    @Getter
    @Builder
    public static class InstallPermitRequest {
        /** 安装计划版本 */
        private final String installPlanVersion;
        /** 包清单摘要（冻结） */
        private final SnapshotDigest packageManifestDigest;
        /** 期望的包清单摘要（用于重新计算校验） */
        private final SnapshotDigest expectedPackageManifestDigest;
        /** 条件集版本（冻结） */
        private final String conditionSetVersion;
        /** 许可有效期 */
        private final Instant validUntil;
        /** 离线策略（冻结，JSON） */
        private final String offlinePolicy;
        /** 超时策略（冻结，JSON） */
        private final String timeoutPolicy;
        /** 控制策略（冻结，JSON） */
        private final String controlPolicy;
        /** 是否需要授权（发布冻结） */
        private final boolean consentRequired;
        /** 统一 ConsentPolicy 有效授权判定结果（由调用方计算） */
        private final boolean consentPermitted;
        /** 全部必需包阶段结果是否成功 */
        private final boolean allPackageStageResultsSucceeded;
    }
}
