package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 车辆升级任务完整过程查询服务（CR-015 §3.3）
 * <p>聚合权威数据（tb_task_vehicle / inventory / consent / package / execution / event /
 * control / ecu_result / upgrade_log / retry_log / delivery_observation）。
 * 过程视图采用批量仓储查询，禁止按事件逐条 N+1 查询；
 * 默认不返回 raw Envelope、payload bytes、下载凭证或完整 VIN。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskVehicleProcessQueryService {

    private final TaskVehicleMapper taskVehicleMapper;
    private final VehicleInventoryMapper vehicleInventoryMapper;
    private final VehicleInventoryItemMapper vehicleInventoryItemMapper;
    private final VehicleTaskConsentMapper vehicleTaskConsentMapper;
    private final VehicleTaskPackageMapper vehicleTaskPackageMapper;
    private final PackageStageResultMapper packageStageResultMapper;
    private final OtaExecutionMapper otaExecutionMapper;
    private final ExecutionEventMapper executionEventMapper;
    private final ExecutionControlMapper executionControlMapper;
    private final ExecutionControlAckMapper executionControlAckMapper;
    private final ExecutionEcuResultMapper executionEcuResultMapper;
    private final UpgradeLogMapper upgradeLogMapper;
    private final TaskVehicleRetryLogMapper taskVehicleRetryLogMapper;
    private final GatewayDeliveryObservationMapper gatewayDeliveryObservationMapper;
    private final TaskAppService taskAppService;
    private final ActivityAppService activityAppService;

    /**
     * 完整过程视图（聚合，无分页）
     */
    public TaskVehicleProcessResult getProcess(Long taskVehicleId) {
        TaskVehiclePo vt = taskVehicleMapper.selectPoById(taskVehicleId);
        if (vt == null) {
            log.warn("车辆升级任务[{}]不存在", taskVehicleId);
            return null;
        }
        Long taskId = vt.getTaskId();
        String vin = vt.getVin();

        // executions + events 批量加载（避免 N+1）
        List<OtaExecutionPo> executions = otaExecutionMapper.selectList(
                new QueryWrapper<OtaExecutionPo>().eq("vehicle_task_id", taskVehicleId)
                        .orderByAsc("attempt_no"));
        List<Long> execDbIds = executions.stream()
                .map(OtaExecutionPo::getId)
                .collect(Collectors.toList());
        Map<Long, List<ExecutionEventPo>> eventsByExec = execDbIds.isEmpty() ? Map.of()
                : executionEventMapper.selectList(
                                new QueryWrapper<ExecutionEventPo>()
                                        .in("execution_id", execDbIds)
                                        .orderByAsc("sequence_no"))
                        .stream()
                        .collect(Collectors.groupingBy(ExecutionEventPo::getExecutionId));
        Long activeExecId = vt.getActiveExecutionId();

        return TaskVehicleProcessResult.builder()
                .taskVehicleId(taskVehicleId)
                .vinMasked(maskVin(vin))
                .vehicleTask(buildVehicleTaskView(vt))
                .inventorySummary(buildInventory(vin))
                .consentSummary(buildConsent(taskVehicleId))
                .packageStages(buildPackageStages(taskVehicleId))
                .executions(executions.stream()
                        .map(e -> toExecutionView(e, eventsByExec.getOrDefault(e.getId(), List.of()), activeExecId))
                        .collect(Collectors.toList()))
                .controls(buildControls(execDbIds))
                .ecuResultSummary(buildEcuResults(execDbIds))
                .logSummary(buildLogSummary(taskId, vin))
                .deliveryObservationSummary(buildDeliveryObservations(vin))
                .build();
    }

    /**
     * 车辆任务下的安装尝试列表（分页子资源）
     */
    public List<ExecutionProcessView> listExecutions(Long taskVehicleId) {
        List<OtaExecutionPo> executions = otaExecutionMapper.selectList(
                new QueryWrapper<OtaExecutionPo>().eq("vehicle_task_id", taskVehicleId)
                        .orderByAsc("attempt_no"));
        List<Long> execDbIds = executions.stream()
                .map(OtaExecutionPo::getId)
                .collect(Collectors.toList());
        Map<Long, List<ExecutionEventPo>> eventsByExec = execDbIds.isEmpty() ? Map.of()
                : executionEventMapper.selectList(
                                new QueryWrapper<ExecutionEventPo>()
                                        .in("execution_id", execDbIds)
                                        .orderByAsc("sequence_no"))
                        .stream()
                        .collect(Collectors.groupingBy(ExecutionEventPo::getExecutionId));
        Long activeExecId = Optional.ofNullable(taskVehicleMapper.selectPoById(taskVehicleId))
                .map(TaskVehiclePo::getActiveExecutionId).orElse(null);
        return executions.stream()
                .map(e -> toExecutionView(e, eventsByExec.getOrDefault(e.getId(), List.of()), activeExecId))
                .collect(Collectors.toList());
    }

    /**
     * 单次执行的事件列表（分页子资源）
     */
    public List<ExecutionEventProcessView> listExecutionEvents(Long executionDbId) {
        return executionEventMapper.selectList(
                        new QueryWrapper<ExecutionEventPo>()
                                .eq("execution_id", executionDbId)
                                .orderByAsc("sequence_no"))
                .stream()
                .map(this::toEventView)
                .collect(Collectors.toList());
    }

    /**
     * 车辆任务重试/续传轨迹（CR-015 §3.3 分页子资源 / §3.4 审计筛选）
     *
     * @param taskVehicleId 车辆任务ID
     * @param beginTime     发生时间下限（可空）
     * @param endTime       发生时间上限（可空）
     * @param stage         阶段 DOWNLOAD/INSTALL（可空）
     * @param result        结果（可空）
     * @param attemptNo     尝试序号（可空）
     */
    public List<TaskVehicleRetryLogResult> listRetryLogs(Long taskVehicleId, Date beginTime, Date endTime,
                                                         String stage, String result, Integer attemptNo) {
        TaskVehiclePo vt = taskVehicleMapper.selectPoById(taskVehicleId);
        if (vt == null) {
            return List.of();
        }
        QueryWrapper<TaskVehicleRetryLogPo> query = new QueryWrapper<TaskVehicleRetryLogPo>()
                .eq("task_id", vt.getTaskId())
                .eq("vin", vt.getVin());
        if (beginTime != null) {
            query.ge("retried_at", beginTime);
        }
        if (endTime != null) {
            query.le("retried_at", endTime);
        }
        if (stage != null && !stage.isBlank()) {
            query.eq("stage", stage);
        }
        if (result != null && !result.isBlank()) {
            query.eq("result", result);
        }
        if (attemptNo != null) {
            query.eq("attempt_no", attemptNo);
        }
        query.orderByDesc("retried_at").orderByDesc("id");
        return taskVehicleRetryLogMapper.selectList(query)
                .stream()
                .map(this::toRetryLogView)
                .collect(Collectors.toList());
    }

    /**
     * 车辆升级日志登记（CR-015 §3.3 分页子资源 / §3.4 审计筛选）
     *
     * @param taskVehicleId 车辆任务ID
     * @param beginTime     上传时间下限（可空）
     * @param endTime       上传时间上限（可空）
     * @param uploadState   上传状态（可空）
     */
    public List<UpgradeLogResult> listUpgradeLogs(Long taskVehicleId, Date beginTime, Date endTime, String uploadState) {
        TaskVehiclePo vt = taskVehicleMapper.selectPoById(taskVehicleId);
        if (vt == null) {
            return List.of();
        }
        QueryWrapper<UpgradeLogPo> query = new QueryWrapper<UpgradeLogPo>()
                .eq("task_id", vt.getTaskId())
                .eq("vin", vt.getVin());
        if (beginTime != null) {
            query.ge("upload_time", beginTime);
        }
        if (endTime != null) {
            query.le("upload_time", endTime);
        }
        if (uploadState != null && !uploadState.isBlank()) {
            query.eq("upload_state", uploadState);
        }
        query.orderByDesc("upload_time").orderByDesc("id");
        return upgradeLogMapper.selectList(query)
                .stream()
                .map(this::toUpgradeLogView)
                .collect(Collectors.toList());
    }

    // ==================== 各摘要构建 ====================

    private TaskVehicleProcessView buildVehicleTaskView(TaskVehiclePo vt) {
        TaskVehicleProcessView.TaskVehicleProcessViewBuilder b = TaskVehicleProcessView.builder()
                .taskId(vt.getTaskId())
                .activityId(vt.getActivityId())
                .taskRevision(vt.getTaskRevision())
                .snapshotDigest(vt.getSnapshotDigest())
                .vehicleTaskStatus(vt.getVehicleTaskStatus())
                .availabilityStatus(vt.getAvailabilityStatus())
                .downloadReadyState(vt.getDownloadReadyState())
                .consentState(vt.getConsentState())
                .releaseAt(toInstant(vt.getReleaseAt()))
                .startTime(toInstant(vt.getVtStartTime()))
                .endTime(toInstant(vt.getVtEndTime()))
                .localDisposition(vt.getLocalDisposition())
                .packageCacheAction(vt.getPackageCacheAction())
                .lastAttemptNo(vt.getLastAttemptNo())
                .activeExecutionId(vt.getActiveExecutionId())
                .downloadRetryCount(vt.getDownloadRetryCount())
                .installRetryCount(vt.getInstallRetryCount())
                .lastFailReason(vt.getLastFailReason());
        try {
            TaskResult task = taskAppService.getTaskById(vt.getTaskId());
            if (task != null) {
                b.taskName(task.getName());
            }
            ActivityPo activity = activityAppService.getActivityById(vt.getActivityId());
            if (activity != null) {
                b.activityName(activity.getName());
            }
        } catch (Exception e) {
            log.debug("过程视图加载任务/活动名称失败: {}", e.getMessage());
        }
        return b.build();
    }

    private InventoryProcessSummary buildInventory(String vin) {
        VehicleInventoryPo inventory = vehicleInventoryMapper.selectList(
                        new QueryWrapper<VehicleInventoryPo>()
                                .eq("vin", vin)
                                .orderByDesc("inventory_revision")
                                .orderByDesc("id")
                                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        if (inventory == null) {
            return null;
        }
        int ecuCount = Math.toIntExact(vehicleInventoryItemMapper.selectCount(
                new QueryWrapper<VehicleInventoryItemPo>().eq("inventory_id", inventory.getId())));
        return InventoryProcessSummary.builder()
                .inventoryRevision(inventory.getInventoryRevision())
                .digest(inventory.getDigest())
                .algorithm(inventory.getAlgorithm())
                .acceptedTime(toInstant(inventory.getAcceptedTime()))
                .ecuCount(ecuCount)
                .build();
    }

    private ConsentProcessSummary buildConsent(Long taskVehicleId) {
        VehicleTaskConsentPo consent = vehicleTaskConsentMapper.selectList(
                        new QueryWrapper<VehicleTaskConsentPo>()
                                .eq("vehicle_task_id", taskVehicleId)
                                .orderByDesc("id")
                                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        if (consent == null) {
            return null;
        }
        return ConsentProcessSummary.builder()
                .consentResult(consent.getConsentResult())
                .receiptId(consent.getConsentReceiptId())
                .taskRevision(consent.getTaskRevision())
                .articleId(consent.getArticleId())
                .articleVersion(consent.getArticleVersion())
                .articleHash(consent.getArticleHash())
                .scopeDigest(consent.getConsentScopeDigest())
                .channel(consent.getChannel())
                .reportedAt(toInstant(consent.getReportedAt()))
                .receivedAt(toInstant(consent.getReceivedAt()))
                .expireAt(toInstant(consent.getExpireAt()))
                .supersedesConsentId(consent.getSupersedesConsentId())
                .sourceModel(consent.getSourceModel())
                .build();
    }

    private List<PackageStageProcessView> buildPackageStages(Long taskVehicleId) {
        List<VehicleTaskPackagePo> packages = vehicleTaskPackageMapper.selectList(
                new QueryWrapper<VehicleTaskPackagePo>().eq("vehicle_task_id", taskVehicleId));
        List<PackageStageResultPo> stageResults = packageStageResultMapper.selectList(
                new QueryWrapper<PackageStageResultPo>().eq("vehicle_task_id", taskVehicleId));
        Map<String, List<PackageStageResultPo>> resultByPackage = stageResults.stream()
                .collect(Collectors.groupingBy(r -> r.getPackageId() != null ? r.getPackageId() : ""));

        return packages.stream().map(pkg -> {
            PackageStageProcessView.PackageStageProcessViewBuilder b = PackageStageProcessView.builder()
                    .packageId(pkg.getPackageId())
                    .packageRevision(pkg.getPackageRevision())
                    .etag(pkg.getEtag())
                    .downloadState(pkg.getDownloadState())
                    .verifyState(pkg.getVerifyState());
            List<PackageStageResultPo> results = resultByPackage.getOrDefault(pkg.getPackageId(), List.of());
            // 取各阶段最新结果（按落库时间）
            PackageStageResultPo latest = results.stream()
                    .max(Comparator.comparing(r -> r.getCreateTime() != null ? r.getCreateTime().getTime() : 0L))
                    .orElse(null);
            if (latest != null) {
                b.signatureResult(latest.getSignatureResult())
                        .decryptResult(latest.getDecryptResult())
                        .stageResultStatus(latest.getResultStatus())
                        .failReason(latest.getFailReason());
            } else if (!results.isEmpty()) {
                PackageStageResultPo any = results.get(0);
                b.signatureResult(any.getSignatureResult())
                        .decryptResult(any.getDecryptResult())
                        .stageResultStatus(any.getResultStatus())
                        .failReason(any.getFailReason());
            }
            return b.build();
        }).collect(Collectors.toList());
    }

    private ExecutionProcessView toExecutionView(OtaExecutionPo e, List<ExecutionEventPo> events, Long activeExecId) {
        Set<Long> receivedSeq = events.stream()
                .map(ExecutionEventPo::getSequenceNo)
                .collect(Collectors.toSet());
        return ExecutionProcessView.builder()
                .id(e.getId())
                .executionId(e.getExecutionId())
                .attemptNo(e.getAttemptNo())
                .status(e.getStatus())
                .taskRevision(e.getTaskRevision())
                .installPlanVersion(e.getInstallPlanVersion())
                .acceptedSequenceNo(e.getAcceptedSequenceNo())
                .finalSequenceNo(e.getFinalSequenceNo())
                .missingSequenceRanges(computeMissingRanges(
                        e.getAcceptedSequenceNo(), e.getFinalSequenceNo(), receivedSeq))
                .active(activeExecId != null && activeExecId.equals(e.getId()))
                .offlinePolicy(e.getOfflinePolicy())
                .timeoutPolicy(e.getTimeoutPolicy())
                .validUntil(toInstant(e.getValidUntil()))
                .build();
    }

    private ControlProcessSummary buildControls(List<Long> execDbIds) {
        if (execDbIds.isEmpty()) {
            return ControlProcessSummary.builder().controlCount(0).build();
        }
        List<ExecutionControlPo> controls = executionControlMapper.selectList(
                new QueryWrapper<ExecutionControlPo>().in("execution_id", execDbIds));
        ExecutionControlPo latest = controls.stream()
                .max(Comparator.comparing(c -> c.getControlRevision() != null ? c.getControlRevision() : 0))
                .orElse(null);

        ExecutionControlAckPo latestAck = null;
        if (latest != null) {
            latestAck = executionControlAckMapper.selectList(
                            new QueryWrapper<ExecutionControlAckPo>().eq("control_id", latest.getControlId()))
                    .stream()
                    .max(Comparator.comparing(a -> a.getAckSequenceNo() != null ? a.getAckSequenceNo() : 0))
                    .orElse(null);
        }

        ControlProcessSummary.ControlProcessSummaryBuilder b = ControlProcessSummary.builder()
                .controlCount(controls.size());
        if (latest != null) {
            b.latestControlRevision(latest.getControlRevision())
                    .latestAction(latest.getAction())
                    .latestScope(latest.getScope())
                    .latestApplyMode(latest.getApplyMode());
        }
        if (latestAck != null) {
            b.latestAckStatus(latestAck.getAckStatus())
                    .latestAckSequenceNo(latestAck.getAckSequenceNo())
                    .latestAckTime(toInstant(latestAck.getAckTime()));
        }
        return b.build();
    }

    private List<EcuResultProcessView> buildEcuResults(List<Long> execDbIds) {
        if (execDbIds.isEmpty()) {
            return List.of();
        }
        return executionEcuResultMapper.selectList(
                        new QueryWrapper<ExecutionEcuResultPo>().in("execution_id", execDbIds))
                .stream()
                .map(r -> EcuResultProcessView.builder()
                        .ecuId(r.getEcuId())
                        .targetSoftwareVersion(r.getTargetSoftwareVersion())
                        .actualSoftwareVersion(r.getActualSoftwareVersion())
                        .result(r.getResult())
                        .failReason(r.getFailReason())
                        .build())
                .collect(Collectors.toList());
    }

    private List<UpgradeLogProcessView> buildLogSummary(Long taskId, String vin) {
        return upgradeLogMapper.selectList(
                        new QueryWrapper<UpgradeLogPo>()
                                .eq("task_id", taskId)
                                .eq("vin", vin)
                                .orderByDesc("upload_time")
                                .orderByDesc("id"))
                .stream()
                .map(l -> UpgradeLogProcessView.builder()
                        .logUrl(l.getLogUrl())
                        .uploadState(l.getUploadState())
                        .uploadTime(toInstant(l.getUploadTime()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<DeliveryObservationProcessView> buildDeliveryObservations(String vin) {
        return gatewayDeliveryObservationMapper.selectByVinHash(sha256(vin), 20)
                .stream()
                .map(d -> DeliveryObservationProcessView.builder()
                        .stage(d.getStage())
                        .outcome(d.getOutcome())
                        .reason(d.getReason())
                        .retryable(d.getRetryable())
                        .retryAfterMs(d.getRetryAfterMs())
                        .occurredAtMs(d.getOccurredAtMs())
                        .build())
                .collect(Collectors.toList());
    }

    private ExecutionEventProcessView toEventView(ExecutionEventPo e) {
        return ExecutionEventProcessView.builder()
                .eventId(e.getEventId())
                .executionId(e.getExecutionId())
                .sequenceNo(e.getSequenceNo())
                .eventType(e.getEventType())
                .eventDigest(e.getEventDigest())
                .disposition(e.getDisposition())
                .receivedTime(toInstant(e.getReceivedTime()))
                .build();
    }

    private TaskVehicleRetryLogResult toRetryLogView(TaskVehicleRetryLogPo po) {
        return TaskVehicleRetryLogResult.builder()
                .taskId(po.getTaskId())
                .vinMasked(maskVin(po.getVin()))
                .stage(po.getStage())
                .attemptNo(po.getAttemptNo())
                .offset(po.getOffset())
                .result(po.getResult())
                .reason(po.getReason())
                .retriedAt(toInstant(po.getRetriedAt()))
                .build();
    }

    private UpgradeLogResult toUpgradeLogView(UpgradeLogPo po) {
        return UpgradeLogResult.builder()
                .taskId(po.getTaskId())
                .vinMasked(maskVin(po.getVin()))
                .logUrl(po.getLogUrl())
                .uploadState(po.getUploadState())
                .uploadTime(toInstant(po.getUploadTime()))
                .build();
    }

    // ==================== 工具 ====================

    /**
     * 计算缺失序列区间（acceptedSequenceNo 为连续水位，finalSequenceNo 为预期终态）
     */
    private String computeMissingRanges(Long accepted, Long finalSeq, Set<Long> received) {
        if (accepted == null || finalSeq == null || accepted >= finalSeq) {
            return "";
        }
        List<long[]> ranges = new ArrayList<>();
        long start = accepted + 1;
        long end = finalSeq;
        long rangeStart = -1;
        for (long seq = start; seq <= end; seq++) {
            boolean present = received.contains(seq);
            if (!present && rangeStart < 0) {
                rangeStart = seq;
            } else if (present && rangeStart >= 0) {
                ranges.add(new long[]{rangeStart, seq - 1});
                rangeStart = -1;
            }
        }
        if (rangeStart >= 0) {
            ranges.add(new long[]{rangeStart, end});
        }
        return ranges.stream()
                .map(r -> r[0] == r[1] ? String.valueOf(r[0]) : r[0] + "-" + r[1])
                .collect(Collectors.joining(","));
    }

    private static String maskVin(String vin) {
        if (vin == null || vin.isBlank()) {
            return "***";
        }
        if (vin.length() <= 4) {
            return "***";
        }
        return "***" + vin.substring(vin.length() - 4);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("VIN SHA-256 计算失败", e);
        }
    }

    private static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    private static Instant toInstant(java.time.LocalDateTime ldt) {
        return ldt != null ? ldt.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
