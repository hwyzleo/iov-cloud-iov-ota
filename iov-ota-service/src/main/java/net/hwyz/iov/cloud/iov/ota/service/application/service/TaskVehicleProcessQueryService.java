package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.*;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRetryLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.UpgradeLogRepository;
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
    private final ExecutionEcuSoftwareUnitMapper executionEcuSoftwareUnitMapper;
    private final VehicleInventorySoftwareUnitMapper vehicleInventorySoftwareUnitMapper;
    private final UpgradeLogRepository upgradeLogRepository;
    private final PackageDownloadProgressMapper packageDownloadProgressMapper;
    private final PackageDownloadProgressEventMapper packageDownloadProgressEventMapper;
    private final TaskVehicleRetryLogRepository taskVehicleRetryLogRepository;
    private final GatewayDeliveryObservationMapper gatewayDeliveryObservationMapper;
    private final TaskVehicleResetLogMapper taskVehicleResetLogMapper;
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
                .logSummary(buildLogSummary(taskVehicleId))
                .deliveryObservationSummary(buildDeliveryObservations(vin))
                .resetLogs(buildResetLogs(taskId, vin))
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
     * 单次执行的事件列表（分页子资源，CR-026 §8.2）。
     *
     * <p>先校验执行确实属于 path 中的车辆任务，防止跨任务读取；只返回白名单结构化字段。</p>
     *
     * @param taskVehicleId 路径车辆任务ID
     * @param executionDbId 执行行主键
     * @param stage         阶段过滤（可空）
     * @param beginTime     车端发生时间下限（可空）
     * @param endTime       车端发生时间上限（可空）
     * @return 事件视图列表（数据库分页由 PageHelper 拦截）
     */
    public List<ExecutionEventProcessView> listExecutionEvents(Long taskVehicleId, Long executionDbId,
                                                               String stage, Date beginTime, Date endTime) {
        OtaExecutionPo execution = otaExecutionMapper.selectById(executionDbId);
        if (execution == null || !taskVehicleId.equals(execution.getVehicleTaskId())) {
            return List.of();
        }
        QueryWrapper<ExecutionEventPo> query = new QueryWrapper<ExecutionEventPo>()
                .eq("execution_id", executionDbId);
        if (stage != null && !stage.isBlank()) {
            query.eq("event_type", stage);
        }
        if (beginTime != null) {
            query.ge("source_occurred_at", beginTime);
        }
        if (endTime != null) {
            query.le("source_occurred_at", endTime);
        }
        query.orderByAsc("sequence_no");
        return executionEventMapper.selectList(query)
                .stream()
                .map(this::toEventView)
                .collect(Collectors.toList());
    }

    /**
     * 包阶段结果历史（CR-026 §8.2：每包各阶段历史行，数据库分页 + 过滤）。
     *
     * @param taskVehicleId 车辆任务ID
     * @param packageId     包ID过滤（可空）
     * @param stage         阶段过滤（可空）
     * @param beginTime     受理时间下限（可空）
     * @param endTime       受理时间上限（可空）
     * @return 阶段历史列表
     */
    public List<PackageStageHistoryResult> listPackageStageResults(Long taskVehicleId, String packageId,
                                                                   String stage, Date beginTime, Date endTime) {
        QueryWrapper<PackageStageResultPo> query = new QueryWrapper<PackageStageResultPo>()
                .eq("vehicle_task_id", taskVehicleId);
        if (packageId != null && !packageId.isBlank()) {
            query.eq("package_id", packageId);
        }
        if (stage != null && !stage.isBlank()) {
            query.eq("stage", stage);
        }
        if (beginTime != null) {
            query.ge("received_at", beginTime);
        }
        if (endTime != null) {
            query.le("received_at", endTime);
        }
        query.orderByAsc("package_id").orderByAsc("id");
        return packageStageResultMapper.selectList(query).stream()
                .map(r -> PackageStageHistoryResult.builder()
                        .id(r.getId())
                        .vehicleTaskId(r.getVehicleTaskId())
                        .packageId(r.getPackageId())
                        .stage(r.getStage())
                        .resultStatus(r.getResultStatus())
                        .packageRevision(r.getPackageRevision())
                        .etag(r.getEtag())
                        .digest(r.getDigest())
                        .signatureResult(r.getSignatureResult())
                        .decryptResult(r.getDecryptResult())
                        .failReason(r.getFailReason())
                        .downloadAttemptNo(r.getDownloadAttemptNo())
                        .occurredAt(toInstant(r.getSourceOccurredAt()))
                        .receivedAt(toInstant(r.getReceivedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 单次下载尝试的进度历史（CR-026 §8.2，数据库分页 + 过滤）。
     *
     * @param taskVehicleId     路径车辆任务ID
     * @param packageId         包ID
     * @param downloadAttemptNo 下载尝试序号
     * @param state             下载状态过滤（可空）
     * @param beginTime         受理时间下限（可空）
     * @param endTime           受理时间上限（可空）
     * @return 进度历史列表
     */
    public List<DownloadProgressEventResult> listDownloadEvents(Long taskVehicleId, String packageId,
                                                                Integer downloadAttemptNo, String state,
                                                                Date beginTime, Date endTime) {
        if (taskVehicleMapper.selectPoById(taskVehicleId) == null) {
            return List.of();
        }
        QueryWrapper<PackageDownloadProgressEventPo> query = new QueryWrapper<PackageDownloadProgressEventPo>()
                .eq("vehicle_task_id", taskVehicleId)
                .eq("package_id", packageId)
                .eq("download_attempt_no", downloadAttemptNo);
        if (state != null && !state.isBlank()) {
            query.eq("download_state", state);
        }
        if (beginTime != null) {
            query.ge("received_at", beginTime);
        }
        if (endTime != null) {
            query.le("received_at", endTime);
        }
        query.orderByAsc("id");
        return packageDownloadProgressEventMapper.selectList(query).stream()
                .map(e -> DownloadProgressEventResult.builder()
                        .id(e.getId())
                        .progressId(e.getProgressId())
                        .downloadAttemptNo(e.getDownloadAttemptNo())
                        .progressSeq(e.getProgressSeq())
                        .downloadState(e.getDownloadState())
                        .downloadedBytes(e.getDownloadedBytes())
                        .totalBytes(e.getTotalBytes())
                        .resumeOffset(e.getResumeOffset())
                        .progressPercent(e.getProgressPercent())
                        .etag(e.getEtag())
                        .errorCode(e.getErrorCode())
                        .failureReason(e.getFailureReason())
                        .disposition(e.getDisposition())
                        .occurredAt(toInstant(e.getSourceOccurredAt()))
                        .receivedAt(toInstant(e.getReceivedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 车辆任务是否存在（子资源路径归属校验）。
     */
    public boolean vehicleTaskExists(Long taskVehicleId) {
        return taskVehicleId != null && taskVehicleMapper.selectPoById(taskVehicleId) != null;
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
                                                         String stage, String action, Integer attemptNo) {
        if (taskVehicleMapper.selectPoById(taskVehicleId) == null) {
            return List.of();
        }
        return taskVehicleRetryLogRepository.listByVehicleTask(taskVehicleId,
                        beginTime == null ? null : beginTime.toInstant(),
                        endTime == null ? null : endTime.toInstant(),
                        stage, action, attemptNo)
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
        if (taskVehicleMapper.selectPoById(taskVehicleId) == null) {
            return List.of();
        }
        return upgradeLogRepository.listByVehicleTask(taskVehicleId,
                        beginTime == null ? null : beginTime.toInstant(),
                        endTime == null ? null : endTime.toInstant(),
                        uploadState)
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
                .lastFailReason(vt.getLastFailReason())
                .supersededBy(vt.getSupersededBy())
                .isCurrent(vt.getSupersededBy() == null);
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
        // CR-019：ECU → SoftwareUnit 明细
        List<VehicleInventoryItemPo> itemPos = vehicleInventoryItemMapper
                .selectByInventoryId(inventory.getId());
        List<InventoryProcessSummary.EcuInventorySummary> ecuList = itemPos.stream()
                .map(item -> {
                    List<VehicleInventorySoftwareUnitPo> unitPos =
                            vehicleInventorySoftwareUnitMapper.selectByInventoryItemId(item.getId());
                    List<InventoryProcessSummary.SoftwareUnitSummary> units = unitPos.stream()
                            .map(u -> InventoryProcessSummary.SoftwareUnitSummary.builder()
                                    .softwareTargetCode(u.getSoftwareTargetCode())
                                    .softwarePartNumber(u.getSoftwarePartNumber())
                                    .swVersion(u.getSwVersion())
                                    .slot(u.getSlot())
                                    .active(u.getActive())
                                    .digest(u.getDigest())
                                    .build())
                            .toList();
                    return InventoryProcessSummary.EcuInventorySummary.builder()
                            .ecuId(item.getEcuId())
                            .hardwarePn(item.getHardwarePn())
                            .hardwareVersion(item.getHardwareVersion())
                            .softwareModel(item.getSoftwareModel())
                            .softwareUnits(units)
                            .build();
                })
                .toList();
        return InventoryProcessSummary.builder()
                .inventoryRevision(inventory.getInventoryRevision())
                .digest(inventory.getDigest())
                .algorithm(inventory.getAlgorithm())
                .acceptedTime(toInstant(inventory.getAcceptedTime()))
                .ecuCount(ecuCount)
                .inventoryModel(deriveModel(itemPos))
                .canonicalizationVersion(inventory.getCanonicalizationVersion())
                .canonicalDigestHex(inventory.getCanonicalDigest() == null ? null
                        : java.util.HexFormat.of().formatHex(inventory.getCanonicalDigest()))
                .sourceCollectedAt(toInstant(inventory.getSourceCollectedAt()))
                .ecuList(ecuList)
                .build();
    }

    private static String deriveModel(List<VehicleInventoryItemPo> itemPos) {
        boolean multi = itemPos.stream()
                .anyMatch(i -> "MULTI_TARGET".equals(i.getSoftwareModel()));
        return multi ? "MULTI_TARGET" : "SINGLE_IMAGE";
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
        // CR-026：批量加载每包当前下载进度快照（取最高尝试号），避免逐包 N+1
        List<PackageDownloadProgressPo> progressRows = packageDownloadProgressMapper.selectList(
                new QueryWrapper<PackageDownloadProgressPo>()
                        .eq("vehicle_task_id", taskVehicleId)
                        .orderByAsc("download_attempt_no"));
        Map<String, PackageDownloadProgressPo> progressByPackage = progressRows.stream()
                .collect(Collectors.toMap(PackageDownloadProgressPo::getPackageId, p -> p,
                        (first, second) -> second));

        if (packages.isEmpty()) {
            // 冻结包清单未落库（存量/联调环境，tb_vehicle_task_package 为空）时，
            // 以阶段结果与进度事实反推包集合，避免「包下载进度/包阶段」区块整体为空
            // （与 CR-028 §4.33 的兜底口径一致）。
            java.util.LinkedHashMap<String, VehicleTaskPackagePo> derived = new java.util.LinkedHashMap<>();
            stageResults.forEach(r -> derived.putIfAbsent(r.getPackageId(),
                    VehicleTaskPackagePo.builder().vehicleTaskId(taskVehicleId)
                            .packageId(r.getPackageId()).packageRevision(r.getPackageRevision()).build()));
            progressRows.forEach(p -> derived.putIfAbsent(p.getPackageId(),
                    VehicleTaskPackagePo.builder().vehicleTaskId(taskVehicleId)
                            .packageId(p.getPackageId()).packageRevision(p.getPackageRevision()).build()));
            packages = new java.util.ArrayList<>(derived.values());
        }
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
            PackageDownloadProgressPo progress = progressByPackage.get(pkg.getPackageId());
            if (progress != null) {
                b.downloadProgress(DownloadProgressProcessView.builder()
                        .downloadAttemptNo(progress.getDownloadAttemptNo())
                        .downloadState(progress.getDownloadState())
                        .latestProgressSeq(progress.getLatestProgressSeq())
                        .downloadedBytes(progress.getDownloadedBytes())
                        .totalBytes(progress.getTotalBytes())
                        .resumeOffset(progress.getResumeOffset())
                        .progressPercent(progress.getProgressPercent())
                        .etag(progress.getEtag())
                        .errorCode(progress.getErrorCode())
                        .failureReason(progress.getFailureReason())
                        .occurredAt(toInstant(progress.getSourceOccurredAt()))
                        .receivedAt(toInstant(progress.getReceivedAt()))
                        .build());
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
                .startTime(toInstant(e.getStartTime()))
                .endTime(toInstant(e.getEndTime()))
                .startReceivedTime(toInstant(e.getStartReceivedTime()))
                .endReceivedTime(toInstant(e.getEndReceivedTime()))
                .durationMs(durationMs(e.getStartTime(), e.getEndTime()))
                .build();
    }

    /**
     * 过程耗时：起止均有效且非负时返回，否则 null（CR-026：禁止负耗时与 1970 展示）。
     */
    private static Long durationMs(Date start, Date end) {
        if (start == null || end == null || end.before(start)) {
            return null;
        }
        return end.getTime() - start.getTime();
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
                .map(r -> {
                    // CR-019：per-Target/Slot 软件单元结果
                    List<ExecutionEcuSoftwareUnitPo> unitPos =
                            executionEcuSoftwareUnitMapper.selectByEcuResultId(r.getId());
                    List<EcuResultProcessView.SoftwareUnitResultView> units = unitPos.stream()
                            .map(u -> EcuResultProcessView.SoftwareUnitResultView.builder()
                                    .softwareTargetCode(u.getSoftwareTargetCode())
                                    .sourceVersion(u.getSourceVersion())
                                    .targetVersion(u.getTargetVersion())
                                    .actualVersion(u.getActualVersion())
                                    .slot(u.getSlot())
                                    .active(u.getActive())
                                    .result(u.getResult())
                                    .failureStage(u.getFailureStage())
                                    .rollbackResult(u.getRollbackResult())
                                    .packageId(u.getPackageId())
                                    .build())
                            .toList();
                    return EcuResultProcessView.builder()
                            .ecuId(r.getEcuId())
                            .softwareModel(r.getSoftwareModel())
                            .targetSoftwareVersion(r.getTargetSoftwareVersion())
                            .actualSoftwareVersion(r.getActualSoftwareVersion())
                            .result(r.getResult())
                            .failReason(r.getFailReason())
                            .softwareUnitResults(units)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<UpgradeLogProcessView> buildLogSummary(Long taskVehicleId) {
        return upgradeLogRepository.listByVehicleTask(taskVehicleId, null, null, null)
                .stream()
                .map(l -> UpgradeLogProcessView.builder()
                        .logRequestId(l.getLogRequestId())
                        .executionId(l.getExecutionId())
                        .objectKey(l.getObjectKey())
                        .objectSizeBytes(l.getObjectSizeBytes())
                        .logDigest(l.getLogDigest())
                        .digestAlgorithm(l.getDigestAlgorithm())
                        .uploadState(l.getUploadState())
                        .failureReason(l.getFailureReason())
                        .logUrl(l.getLogUrl())
                        .authorizedAt(l.getAuthorizedAt())
                        .uploadedAt(l.getUploadedAt())
                        .receivedAt(l.getReceivedAt())
                        .uploadTime(l.getUploadTime())
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

    /**
     * 重置审计链（IOV-OTA-DSN-CR-025 §4.29.7）：返回与该车辆任务直接相关的重置追加记录。
     */
    private List<VehicleTaskResetLogResult> buildResetLogs(Long taskId, String vin) {
        // 重置链按 task + vin 返回完整链路（3→4→5→6→7），
        // 而非只返回与当前车辆任务相邻的一跳。
        return taskVehicleResetLogMapper.selectList(
                        new QueryWrapper<TaskVehicleResetLogPo>()
                                .eq("task_id", taskId)
                                .eq("vin", vin)
                                .eq("row_valid", 1)
                                .orderByAsc("id"))
                .stream()
                .map(l -> VehicleTaskResetLogResult.builder()
                        .id(l.getId())
                        .oldVehicleTaskId(l.getOldVehicleTaskId())
                        .newVehicleTaskId(l.getNewVehicleTaskId())
                        .oldStatus(l.getOldStatus())
                        .oldDownloadReady(l.getOldDownloadReady())
                        .oldConsentState(l.getOldConsentState())
                        .oldAttemptNo(l.getOldAttemptNo())
                        .oldExecutionId(l.getOldExecutionId())
                        .oldExecutionStatus(l.getOldExecutionStatus())
                        .force(l.getForceFlag() != null && l.getForceFlag() == 1)
                        .reason(l.getReason())
                        .result(l.getResult())
                        .operator(l.getCreateBy())
                        .createdAt(l.getCreateTime() != null ? l.getCreateTime().toInstant() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private ExecutionEventProcessView toEventView(ExecutionEventPo e) {
        return ExecutionEventProcessView.builder()
                .eventId(e.getEventId())
                .executionId(e.getExecutionId())
                .sequenceNo(e.getSequenceNo())
                .stage(e.getEventType())
                .eventType(e.getEventType())
                .eventDigest(e.getEventDigest())
                .disposition(e.getDisposition())
                .receivedTime(toInstant(e.getReceivedTime()))
                .occurredAt(toInstant(e.getSourceOccurredAt()))
                .progressPercent(e.getProgressPercent())
                .ecuId(e.getEcuId())
                .softwareTargetCode(e.getSoftwareTargetCode())
                .slot(e.getSlot())
                .errorCode(e.getErrorCode())
                .failureReason(e.getFailureReason())
                .build();
    }

    private TaskVehicleRetryLogResult toRetryLogView(
            net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleRetryLog po) {
        return TaskVehicleRetryLogResult.builder()
                .taskId(po.getTaskId())
                .vinMasked(maskVin(po.getVin()))
                .stage(po.getStage())
                .action(po.getAction())
                .attemptNo(po.getAttemptNo())
                .offset(po.getOffset())
                .result(po.getResult())
                .reason(po.getReason())
                .errorCode(po.getErrorCode())
                .retriedAt(po.getRetriedAt())
                .occurredAt(po.getSourceOccurredAt())
                .receivedAt(po.getReceivedAt())
                .build();
    }

    private UpgradeLogResult toUpgradeLogView(
            net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradeLog log) {
        return UpgradeLogResult.builder()
                .taskId(log.getTaskId())
                .vinMasked(maskVin(log.getVin()))
                .vehicleTaskId(log.getVehicleTaskId())
                .executionId(log.getExecutionId())
                .logRequestId(log.getLogRequestId())
                .logScope(log.getLogScope())
                .desensitizeVersion(log.getDesensitizeVersion())
                .privacyLevel(log.getPrivacyLevel())
                .objectKey(log.getObjectKey())
                .logObjectId(log.getLogObjectId())
                .objectSizeBytes(log.getObjectSizeBytes())
                .logDigest(log.getLogDigest())
                .digestAlgorithm(log.getDigestAlgorithm())
                .uploadState(log.getUploadState())
                .uploadResult(log.getUploadResult())
                .failureReason(log.getFailureReason())
                .logUrl(log.getLogUrl())
                .authorizedAt(log.getAuthorizedAt())
                .uploadedAt(log.getUploadedAt())
                .receivedAt(log.getReceivedAt())
                .uploadTime(log.getUploadTime())
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
