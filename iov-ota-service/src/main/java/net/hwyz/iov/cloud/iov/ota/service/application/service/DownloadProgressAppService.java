package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DownloadProgressCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DownloadProgressResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.DownloadProgressDecisionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PackageDownloadProgressEventMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PackageDownloadProgressMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageDownloadProgressEventPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageDownloadProgressPo;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskPackagePo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 下载进度受理服务（IOV-OTA-DSN-CR-026 §5.3、§7.1；PAR-PROTO-DSN-CR-011 冻结语义）。
 *
 * <p>边界：下载进度属于 VehicleTask + Package + downloadAttemptNo 的观测事实，**不创建
 * Execution、不进入执行事件水位、不推进 READY_TO_INSTALL**；只有 COMPLETED 字节完成不算就绪。</p>
 *
 * <p>幂等：progressId 为第一幂等键，首次处置（含 REJECTED/CONFLICT）写入
 * {@code tb_idempotency_record}，以便重投复用原结果且不占用进度第二唯一键；
 * (vehicle_task_id, package_id, download_attempt_no, progress_seq) 由历史表唯一键裁决。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class DownloadProgressAppService {

    /** 幂等作用域 */
    public static final String DECISION_SCOPE = "DOWNLOAD_PROGRESS";

    public static final String DISPOSITION_CURRENT = "CURRENT";
    public static final String DISPOSITION_LATE = "LATE_OBSERVATION";
    public static final String DISPOSITION_DUPLICATE = "DUPLICATE";
    public static final String DISPOSITION_CONFLICT = "CONFLICT";
    public static final String DISPOSITION_REJECTED = "REJECTED";

    /** 稳定拒绝原因（与 PAR-PROTO-DSN-CR-011 deny registry 对齐，作用域类原因复用既有值） */
    public static final String DENY_PROGRESS_ID_MISSING = "PROGRESS_ID_MISSING";
    public static final String DENY_VEHICLE_TASK_ID_MISSING = "VEHICLE_TASK_ID_MISSING";
    public static final String DENY_PACKAGE_ID_MISSING = "PACKAGE_ID_MISSING";
    public static final String DENY_PACKAGE_REVISION_MISSING = "PACKAGE_REVISION_MISSING";
    public static final String DENY_ATTEMPT_INVALID = "DOWNLOAD_ATTEMPT_NO_INVALID";
    public static final String DENY_PROGRESS_SEQ_INVALID = "PROGRESS_SEQ_INVALID";
    public static final String DENY_TOTAL_BYTES_INVALID = "TOTAL_BYTES_INVALID";
    public static final String DENY_DOWNLOADED_BYTES_INVALID = "DOWNLOADED_BYTES_INVALID";
    public static final String DENY_RESUME_OFFSET_INVALID = "RESUME_OFFSET_INVALID";
    public static final String DENY_OCCURRED_AT_INVALID = "OCCURRED_AT_INVALID";
    public static final String DENY_STATE_UNSUPPORTED = "DOWNLOAD_STATE_UNSUPPORTED";
    public static final String DENY_DIGEST_ALGORITHM_UNSUPPORTED = "DIGEST_ALGORITHM_UNSUPPORTED";

    /** CR-028：冻结包清单记录缺失、按实时投影受理时计入的低基数指标 */
    public static final String METRIC_FREEZE_MISSING = "download_progress_freeze_missing";
    public static final String DENY_DIGEST_VALUE_INVALID = "DIGEST_VALUE_INVALID";
    public static final String DENY_COMPLETED_BYTES_MISMATCH = "COMPLETED_BYTES_MISMATCH";
    public static final String DENY_RESET_BASELINE_MISMATCH = "RESET_OFFSET_BASELINE_MISMATCH";
    public static final String DENY_RESET_UNAUTHORIZED = "RESET_OFFSET_UNAUTHORIZED";
    public static final String DENY_OBJECT_BOUNDARY_CHANGED = "OBJECT_BOUNDARY_CHANGED";
    public static final String DENY_BYTES_REGRESSION = "BYTES_REGRESSION";
    public static final String DENY_SECOND_KEY_CONFLICT = "SECOND_KEY_CONFLICT";
    public static final String DENY_DIGEST_CONFLICT = "DIGEST_CONFLICT";

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String STATE_RESET_OFFSET = "RESET_OFFSET";
    private static final String STATE_COMPLETED = "COMPLETED";
    private static final java.util.Set<String> SUPPORTED_STATES = java.util.Set.of(
            "STARTED", "PROGRESS", "PAUSED", "RESUMED", STATE_RESET_OFFSET, "FAILED", STATE_COMPLETED);

    private final VehicleTaskRepository vehicleTaskRepository;
    private final VehicleTaskPackageMapper vehicleTaskPackageMapper;
    private final SoftwarePackageMapper softwarePackageMapper;
    private final PackageDownloadProgressMapper snapshotMapper;
    private final PackageDownloadProgressEventMapper eventMapper;
    private final DownloadProgressDecisionMapper decisionMapper;
    private final RetryTrajectoryService retryTrajectoryService;
    private final KafkaMessagingMetricsService metrics;
    private final ObjectMapper objectMapper;
    /** CR-028：冻结包清单缺失时与 TaskCheck 下发同源的必需包集合来源 */
    private final VehicleTaskPackageService vehicleTaskPackageService;

    public DownloadProgressAppService(VehicleTaskRepository vehicleTaskRepository,
                                      VehicleTaskPackageMapper vehicleTaskPackageMapper,
                                      SoftwarePackageMapper softwarePackageMapper,
                                      PackageDownloadProgressMapper snapshotMapper,
                                      PackageDownloadProgressEventMapper eventMapper,
                                      DownloadProgressDecisionMapper decisionMapper,
                                      RetryTrajectoryService retryTrajectoryService,
                                      KafkaMessagingMetricsService metrics,
                                      ObjectMapper objectMapper,
                                      VehicleTaskPackageService vehicleTaskPackageService) {
        this.vehicleTaskRepository = vehicleTaskRepository;
        this.vehicleTaskPackageMapper = vehicleTaskPackageMapper;
        this.softwarePackageMapper = softwarePackageMapper;
        this.snapshotMapper = snapshotMapper;
        this.eventMapper = eventMapper;
        this.decisionMapper = decisionMapper;
        this.retryTrajectoryService = retryTrajectoryService;
        this.metrics = metrics;
        this.vehicleTaskPackageService = vehicleTaskPackageService;
        this.objectMapper = objectMapper;
    }

    /**
     * 受理一条下载进度（幂等、单调、乱序分类，全部在同一业务事务内）。
     *
     * @param cmd 进度命令
     * @return 受理结果
     */
    @Transactional
    public DownloadProgressResult receive(DownloadProgressCmd cmd) {
        // 1) progressId 第一幂等键：已在第一次处置中给出结论时直接复用
        DownloadProgressResult replay = replayDecision(cmd);
        if (replay != null) {
            metrics.increment("download_progress_duplicate");
            return replay;
        }
        // 2) 报文合法性与业务作用域
        String deny = validate(cmd);
        if (deny == null) {
            deny = validateScope(cmd);
        }
        if (deny != null) {
            return recordDecision(cmd, null, DownloadProgressResult.builder()
                    .accepted(false).disposition(DISPOSITION_REJECTED)
                    .latestAcceptedProgressSeq(currentSeq(cmd)).denyReason(deny).build());
        }

        PackageDownloadProgressPo snapshot = snapshotMapper.selectForUpdate(
                cmd.getVehicleTaskId(), cmd.getPackageId(), cmd.getDownloadAttemptNo());
        deny = validateAgainstSnapshot(cmd, snapshot);
        if (deny != null) {
            return recordDecision(cmd, snapshot, DownloadProgressResult.builder()
                    .accepted(false).disposition(DISPOSITION_REJECTED)
                    .latestAcceptedProgressSeq(seqOf(snapshot)).denyReason(deny).build());
        }
        boolean current = snapshot == null || cmd.getProgressSeq() > seqOf(snapshot);
        if (!current && snapshot != null && cmd.getProgressSeq().equals(seqOf(snapshot))) {
            return recordDecision(cmd, snapshot, DownloadProgressResult.builder()
                    .accepted(false).disposition(DISPOSITION_CONFLICT)
                    .latestAcceptedProgressSeq(seqOf(snapshot)).denyReason(DENY_SECOND_KEY_CONFLICT).build());
        }

        // 3) 追加不可变历史（第二唯一键冲突 → CONFLICT）
        PackageDownloadProgressEventPo event = toEvent(cmd, snapshot, current
                ? DISPOSITION_CURRENT : DISPOSITION_LATE);
        try {
            eventMapper.insert(event);
        } catch (DuplicateKeyException conflict) {
            metrics.increment("download_progress_conflict");
            return recordDecision(cmd, snapshot, DownloadProgressResult.builder()
                    .accepted(false).disposition(DISPOSITION_CONFLICT)
                    .latestAcceptedProgressSeq(seqOf(snapshot)).denyReason(DENY_SECOND_KEY_CONFLICT).build());
        }

        // 4) 当前快照只在更高序号推进；迟到观测只留历史
        if (snapshot == null) {
            snapshot = newSnapshot(cmd);
            try {
                snapshotMapper.insert(snapshot);
            } catch (DuplicateKeyException race) {
                snapshot = snapshotMapper.selectForUpdate(
                        cmd.getVehicleTaskId(), cmd.getPackageId(), cmd.getDownloadAttemptNo());
                current = snapshot != null && cmd.getProgressSeq() > seqOf(snapshot);
            }
        }
        if (current) {
            advanceSnapshot(cmd);
            recordDownloadRetryFact(cmd);
        }
        DownloadProgressResult result = DownloadProgressResult.builder()
                .accepted(true)
                .disposition(current ? DISPOSITION_CURRENT : DISPOSITION_LATE)
                .latestAcceptedProgressSeq(current ? cmd.getProgressSeq() : Math.max(seqOf(snapshot),
                        cmd.getProgressSeq()))
                .build();
        recordDecision(cmd, snapshot, result);
        log.info("车辆任务[{}]包[{}]下载进度[{}]受理为[{}]，seq[{}] bytes[{}/{}]",
                cmd.getVehicleTaskId(), cmd.getPackageId(), cmd.getProgressId(), result.getDisposition(),
                cmd.getProgressSeq(), cmd.getDownloadedBytes(), cmd.getTotalBytes());
        return result;
    }

    // ------------------------------------------------------------------ 校验

    private String validate(DownloadProgressCmd cmd) {
        if (isBlank(cmd.getProgressId())) {
            return DENY_PROGRESS_ID_MISSING;
        }
        if (cmd.getVehicleTaskId() == null) {
            return DENY_VEHICLE_TASK_ID_MISSING;
        }
        if (isBlank(cmd.getPackageId())) {
            return DENY_PACKAGE_ID_MISSING;
        }
        if (isBlank(cmd.getPackageRevision())) {
            return DENY_PACKAGE_REVISION_MISSING;
        }
        if (cmd.getDownloadAttemptNo() == null || cmd.getDownloadAttemptNo() < 1) {
            return DENY_ATTEMPT_INVALID;
        }
        if (cmd.getProgressSeq() == null || cmd.getProgressSeq() < 1) {
            return DENY_PROGRESS_SEQ_INVALID;
        }
        if (cmd.getDownloadState() == null || !SUPPORTED_STATES.contains(cmd.getDownloadState())) {
            return DENY_STATE_UNSUPPORTED;
        }
        if (cmd.getTotalBytes() == null || cmd.getTotalBytes() <= 0) {
            return DENY_TOTAL_BYTES_INVALID;
        }
        if (cmd.getDownloadedBytes() == null || cmd.getDownloadedBytes() < 0
                || cmd.getDownloadedBytes() > cmd.getTotalBytes()) {
            return DENY_DOWNLOADED_BYTES_INVALID;
        }
        if (cmd.getResumeOffset() == null || cmd.getResumeOffset() < 0
                || cmd.getResumeOffset() > cmd.getDownloadedBytes()) {
            return DENY_RESUME_OFFSET_INVALID;
        }
        if (cmd.getEventDigest() == null || !SHA256.matcher(cmd.getEventDigest()).matches()) {
            return DENY_DIGEST_VALUE_INVALID;
        }
        if (STATE_COMPLETED.equals(cmd.getDownloadState())
                && !cmd.getDownloadedBytes().equals(cmd.getTotalBytes())) {
            return DENY_COMPLETED_BYTES_MISMATCH;
        }
        if (STATE_RESET_OFFSET.equals(cmd.getDownloadState())
                && !cmd.getDownloadedBytes().equals(cmd.getResumeOffset())) {
            return DENY_RESET_BASELINE_MISMATCH;
        }
        return null;
    }

    /**
     * 业务作用域：车辆任务当前性、包冻结、修订/大小/对象边界。
     *
     * <p>PAR-PROTO 只冻结报文级语义；车辆任务/包清单/权威大小属云端业务作用域，失败一律
     * REJECTED 并复用已冻结的稳定原因（不新增 deny_reason）。</p>
     */
    private String validateScope(DownloadProgressCmd cmd) {
        Optional<VehicleTask> vehicleTask = vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId()));
        if (vehicleTask.isEmpty() || vehicleTask.get().getSupersededBy() != null) {
            return DENY_VEHICLE_TASK_ID_MISSING;
        }
        VehicleTaskPackagePo frozen = frozenPackage(cmd.getVehicleTaskId(), cmd.getPackageId());
        if (frozen == null) {
            // CR-028（IOV-OTA-DSN-CR-028 §4.33）：存量/联调环境可能尚未落库冻结包清单
            // （tb_vehicle_task_package 为空）。此时回退到与 TaskCheck 下发 package_list
            // 同源的实时投影判定归属，fail-open + WARN + 指标（与 CR-024 §4.28.4 对
            // snapshot_digest 的处理口径一致）；归属不成立的包仍拒绝。
            if (!vehicleTaskPackageService.requiredPackageIds(vehicleTask.get())
                    .contains(cmd.getPackageId())) {
                return DENY_PACKAGE_ID_MISSING;
            }
            metrics.increment(METRIC_FREEZE_MISSING);
            log.warn("车辆任务[{}]包[{}]未落库冻结包清单记录，按实时投影受理下载进度（fail-open），vin={}",
                    cmd.getVehicleTaskId(), cmd.getPackageId(), cmd.getVin());
        } else {
            if (isNotBlank(frozen.getPackageRevision())
                    && !frozen.getPackageRevision().equals(cmd.getPackageRevision())) {
                return DENY_OBJECT_BOUNDARY_CHANGED;
            }
            if (isNotBlank(cmd.getEtag()) && isNotBlank(frozen.getEtag())
                    && !frozen.getEtag().equals(cmd.getEtag())) {
                return DENY_OBJECT_BOUNDARY_CHANGED;
            }
        }
        Long authoritativeSize = authoritativePackageSize(cmd.getPackageId());
        if (authoritativeSize == null || authoritativeSize <= 0
                || !authoritativeSize.equals(cmd.getTotalBytes())) {
            // US-020 fail-closed：包大小未知/非法或与权威大小不一致时禁止受理
            return DENY_TOTAL_BYTES_INVALID;
        }
        return null;
    }

    private String validateAgainstSnapshot(DownloadProgressCmd cmd, PackageDownloadProgressPo snapshot) {
        if (snapshot == null) {
            if (STATE_RESET_OFFSET.equals(cmd.getDownloadState())
                    && cmd.getResumeOffset() > 0 && !authorizedResetOffset(cmd)) {
                return DENY_RESET_UNAUTHORIZED;
            }
            return null;
        }
        if (STATE_RESET_OFFSET.equals(cmd.getDownloadState())) {
            if (cmd.getResumeOffset() > 0 && !authorizedResetOffset(cmd)) {
                return DENY_RESET_UNAUTHORIZED;
            }
            return null;
        }
        boolean etagChanged = isNotBlank(cmd.getEtag()) && isNotBlank(snapshot.getEtag())
                && !snapshot.getEtag().equals(cmd.getEtag());
        boolean revisionChanged = isNotBlank(snapshot.getPackageRevision())
                && !snapshot.getPackageRevision().equals(cmd.getPackageRevision());
        if (etagChanged || revisionChanged) {
            return DENY_OBJECT_BOUNDARY_CHANGED;
        }
        if (cmd.getProgressSeq() > seqOf(snapshot)
                && (cmd.getDownloadedBytes() < snapshot.getDownloadedBytes()
                || cmd.getResumeOffset() < snapshot.getResumeOffset())) {
            return DENY_BYTES_REGRESSION;
        }
        return null;
    }

    /**
     * 非零 RESET_OFFSET 必须命中最近一次授权 offset 且未过期（PAR-PROTO-DSN-CR-011 §6）。
     */
    private boolean authorizedResetOffset(DownloadProgressCmd cmd) {
        VehicleTaskPackagePo frozen = frozenPackage(cmd.getVehicleTaskId(), cmd.getPackageId());
        if (frozen == null || frozen.getGrantOffset() == null
                || !frozen.getGrantOffset().equals(cmd.getResumeOffset())
                || !Boolean.TRUE.equals(frozen.getGrantResumeAllowed())) {
            return false;
        }
        if (isNotBlank(frozen.getGrantPackageRevision())
                && !frozen.getGrantPackageRevision().equals(cmd.getPackageRevision())) {
            return false;
        }
        if (isNotBlank(cmd.getEtag()) && isNotBlank(frozen.getGrantEtag())
                && !frozen.getGrantEtag().equals(cmd.getEtag())) {
            return false;
        }
        return frozen.getGrantExpiresAt() == null || frozen.getGrantExpiresAt().toInstant().isAfter(Instant.now());
    }

    // ------------------------------------------------------------------ 幂等与落库

    private DownloadProgressResult replayDecision(DownloadProgressCmd cmd) {
        if (isBlank(cmd.getProgressId())) {
            return null;
        }
        Map<String, Object> row = decisionMapper.selectDecision(DECISION_SCOPE, cmd.getProgressId());
        if (row == null || row.isEmpty()) {
            return null;
        }
        String digest = String.valueOf(row.get("request_digest"));
        if (!digest.equals(cmd.getEventDigest())) {
            return new DownloadProgressResult(false, DISPOSITION_CONFLICT,
                    currentSeq(cmd), DENY_DIGEST_CONFLICT);
        }
        DownloadProgressResult first = readSnapshot(String.valueOf(row.get("response_snapshot")));
        if (first == null) {
            return null;
        }
        if (DISPOSITION_REJECTED.equals(first.getDisposition())
                || DISPOSITION_CONFLICT.equals(first.getDisposition())) {
            return first;
        }
        return new DownloadProgressResult(first.isAccepted(), DISPOSITION_DUPLICATE,
                first.getLatestAcceptedProgressSeq(), null);
    }

    private DownloadProgressResult recordDecision(DownloadProgressCmd cmd,
                                                  PackageDownloadProgressPo snapshot,
                                                  DownloadProgressResult result) {
        try {
            decisionMapper.insertDecision(DECISION_SCOPE, cmd.getProgressId(), cmd.getEventDigest(),
                    writeSnapshot(result), cmd.getVin());
        } catch (DuplicateKeyException concurrent) {
            DownloadProgressResult first = replayDecision(cmd);
            return first == null ? result : first;
        }
        return result;
    }

    private String writeSnapshot(DownloadProgressResult result) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("accepted", result.isAccepted());
            map.put("disposition", result.getDisposition());
            map.put("latestAcceptedProgressSeq", result.getLatestAcceptedProgressSeq());
            map.put("denyReason", result.getDenyReason());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("下载进度处置快照序列化失败", e);
        }
    }

    private DownloadProgressResult readSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return new DownloadProgressResult(node.path("accepted").asBoolean(),
                    node.path("disposition").asText(null),
                    node.path("latestAcceptedProgressSeq").asLong(0L),
                    node.path("denyReason").isNull() ? null : node.path("denyReason").asText(null));
        } catch (Exception e) {
            log.warn("下载进度处置快照解析失败：{}", e.getMessage());
            return null;
        }
    }

    private void advanceSnapshot(DownloadProgressCmd cmd) {
        decisionMapper.advanceSnapshot(cmd.getVehicleTaskId(), cmd.getPackageId(), cmd.getDownloadAttemptNo(),
                cmd.getProgressId(), cmd.getProgressSeq(), cmd.getDownloadState(), cmd.getDownloadedBytes(),
                cmd.getTotalBytes(), cmd.getResumeOffset(), percent(cmd), cmd.getEtag(), cmd.getErrorCode(),
                cmd.getFailureReason(), toDate(cmd.getOccurredAt()), new Date());
    }

    /**
     * 续传/重置偏移形成不可变业务轨迹（CR-026 §7.4）。
     */
    private void recordDownloadRetryFact(DownloadProgressCmd cmd) {
        String action = switch (cmd.getDownloadState()) {
            case "RESUME" -> RetryTrajectoryService.ACTION_RESUME;
            case STATE_RESET_OFFSET -> RetryTrajectoryService.ACTION_RESET_OFFSET;
            default -> null;
        };
        if (action == null) {
            return;
        }
        Optional<VehicleTask> vehicleTask = vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId()));
        vehicleTask.ifPresent(vt -> retryTrajectoryService.recordDownloadOperation(vt, cmd.getPackageId(),
                cmd.getDownloadAttemptNo(), action, cmd.getResumeOffset(),
                RetryTrajectoryService.RESULT_SUCCESS, null, cmd.getErrorCode(), cmd.getOccurredAt(), Instant.now()));
    }

    private PackageDownloadProgressPo newSnapshot(DownloadProgressCmd cmd) {
        return PackageDownloadProgressPo.builder()
                .vehicleTaskId(cmd.getVehicleTaskId())
                .packageId(cmd.getPackageId())
                .packageRevision(cmd.getPackageRevision())
                .downloadAttemptNo(cmd.getDownloadAttemptNo())
                .latestProgressId(cmd.getProgressId())
                .latestProgressSeq(0L)
                .downloadState(cmd.getDownloadState())
                .downloadedBytes(0L)
                .totalBytes(cmd.getTotalBytes())
                .resumeOffset(0L)
                .progressPercent(BigDecimal.ZERO)
                .receivedAt(new Date())
                .createTime(new Date())
                .modifyTime(new Date())
                .rowVersion(1)
                .rowValid(true)
                .description("CR-026 下载进度当前快照")
                .build();
    }

    private PackageDownloadProgressEventPo toEvent(DownloadProgressCmd cmd, PackageDownloadProgressPo snapshot,
                                                   String disposition) {
        return PackageDownloadProgressEventPo.builder()
                .progressId(cmd.getProgressId())
                .vehicleTaskId(cmd.getVehicleTaskId())
                .packageId(cmd.getPackageId())
                .packageRevision(cmd.getPackageRevision())
                .downloadAttemptNo(cmd.getDownloadAttemptNo())
                .progressSeq(cmd.getProgressSeq())
                .downloadState(cmd.getDownloadState())
                .downloadedBytes(cmd.getDownloadedBytes())
                .totalBytes(cmd.getTotalBytes())
                .resumeOffset(cmd.getResumeOffset())
                .progressPercent(percent(cmd))
                .etag(cmd.getEtag())
                .errorCode(cmd.getErrorCode())
                .failureReason(cmd.getFailureReason())
                .eventDigest(cmd.getEventDigest())
                .disposition(disposition)
                .sourceOccurredAt(toDate(cmd.getOccurredAt()))
                .receivedAt(new Date())
                .createTime(new Date())
                .modifyTime(new Date())
                .rowVersion(1)
                .rowValid(true)
                .description("CR-026 下载进度不可变历史")
                .build();
    }

    private Long currentSeq(DownloadProgressCmd cmd) {
        PackageDownloadProgressPo snapshot = snapshotMapper.selectOne(new QueryWrapper<PackageDownloadProgressPo>()
                .eq("vehicle_task_id", cmd.getVehicleTaskId())
                .eq("package_id", cmd.getPackageId())
                .eq("download_attempt_no", cmd.getDownloadAttemptNo())
                .last("LIMIT 1"));
        return seqOf(snapshot);
    }

    private VehicleTaskPackagePo frozenPackage(Long vehicleTaskId, String packageId) {
        return vehicleTaskPackageMapper.selectByVehicleTaskId(vehicleTaskId).stream()
                .filter(p -> packageId.equals(p.getPackageId()))
                .findFirst().orElse(null);
    }

    private Long authoritativePackageSize(String packageId) {
        return softwarePackageMapper.selectPackageSizeByCode(packageId);
    }

    private static BigDecimal percent(DownloadProgressCmd cmd) {
        if (cmd.getTotalBytes() == null || cmd.getTotalBytes() <= 0 || cmd.getDownloadedBytes() == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cmd.getDownloadedBytes()).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(cmd.getTotalBytes()), 2, RoundingMode.HALF_UP);
    }

    private static long seqOf(PackageDownloadProgressPo snapshot) {
        return snapshot == null || snapshot.getLatestProgressSeq() == null
                ? 0L : snapshot.getLatestProgressSeq();
    }

    private static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private static boolean isNotBlank(String v) {
        return v != null && !v.isBlank();
    }
}
