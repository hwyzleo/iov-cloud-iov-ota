package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleRetryLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRetryLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.StringJoiner;

/**
 * 重试/续传轨迹写入服务（IOV-OTA-DSN-CR-026 §6.6、§7.4）。
 *
 * <p>轨迹由**业务消息身份**驱动，而不是从 Kafka/MQTT 技术重投次数推断：同一业务事实
 * 重复投递得到同一 {@code retryEventId}，写入按幂等键复用既有不可变行（不追加、不放大
 * retryCount）；只有真实业务重试/续传/重置偏移/新执行才产生新行。
 *
 * <p>写入必须与业务状态变更同事务，由调用方事务边界保证。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryTrajectoryService {

    /** 阶段：下载 */
    public static final String STAGE_DOWNLOAD = "DOWNLOAD";
    /** 阶段：安装 */
    public static final String STAGE_INSTALL = "INSTALL";

    /** 动作：业务重试 */
    public static final String ACTION_RETRY = "RETRY";
    /** 动作：断点续传 */
    public static final String ACTION_RESUME = "RESUME";
    /** 动作：重置偏移 */
    public static final String ACTION_RESET_OFFSET = "RESET_OFFSET";
    /** 动作：新安装执行创建 */
    public static final String ACTION_EXECUTION_CREATED = "EXECUTION_CREATED";

    /** 结果：成功/已受理 */
    public static final String RESULT_SUCCESS = "SUCCESS";
    /** 结果：失败触发 */
    public static final String RESULT_FAIL = "FAIL";

    private static final String EVENT_ID_PREFIX = "rt-";
    private static final int EVENT_ID_HEX_LENGTH = 32;

    private final TaskVehicleRetryLogRepository retryLogRepository;
    private final KafkaMessagingMetricsService metrics;

    /**
     * 记录新安装执行创建（attemptNo &gt; 1 视为安装业务重试）。
     *
     * @param vehicleTask 车辆任务
     * @param attemptNo   安装尝试序号
     * @param receivedAt  云端受理时间
     */
    public void recordExecutionCreated(VehicleTask vehicleTask, int attemptNo, Instant receivedAt) {
        String action = attemptNo > 1 ? ACTION_RETRY : ACTION_EXECUTION_CREATED;
        append(vehicleTask, null, STAGE_INSTALL, action, attemptNo, null,
                RESULT_SUCCESS, null, null, null, receivedAt);
    }

    /**
     * 记录下载阶段业务操作（RESUME / RESET_OFFSET / 失败重试）。
     *
     * @param vehicleTask      车辆任务
     * @param packageId        包ID
     * @param downloadAttemptNo 下载尝试序号
     * @param action           动作（RESUME/RESET_OFFSET/RETRY）
     * @param offset           当次字节偏移
     * @param result           结果（SUCCESS/FAIL）
     * @param reason           原因（可空）
     * @param errorCode        车端错误码（可空）
     * @param sourceOccurredAt 车端发生时间（未知为 null）
     * @param receivedAt       云端受理时间
     */
    public void recordDownloadOperation(VehicleTask vehicleTask, String packageId, int downloadAttemptNo,
                                        String action, Long offset, String result, String reason,
                                        String errorCode, Instant sourceOccurredAt, Instant receivedAt) {
        append(vehicleTask, packageId, STAGE_DOWNLOAD, action, downloadAttemptNo, offset,
                result, reason, errorCode, sourceOccurredAt, receivedAt);
    }

    /**
     * 计算确定性 retryEventId（同一业务事实恒定）。
     *
     * @param vehicleTaskId 车辆任务ID
     * @param packageId     包ID（安装阶段为 null）
     * @param stage         阶段
     * @param action        动作
     * @param attemptNo     尝试序号
     * @param offset        偏移
     * @param trigger       业务触发身份（如包阶段结果ID、执行ID）
     * @return retryEventId
     */
    public static String eventId(Long vehicleTaskId, String packageId, String stage, String action,
                                 Integer attemptNo, Long offset, String trigger) {
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(String.valueOf(vehicleTaskId))
                .add(packageId == null ? "" : packageId)
                .add(stage)
                .add(action)
                .add(attemptNo == null ? "" : String.valueOf(attemptNo))
                .add(offset == null ? "" : String.valueOf(offset))
                .add(trigger == null ? "" : trigger);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(joiner.toString().getBytes(StandardCharsets.UTF_8));
            return EVENT_ID_PREFIX + HexFormat.of().formatHex(hash).substring(0, EVENT_ID_HEX_LENGTH);
        } catch (Exception e) {
            throw new IllegalStateException("retryEventId 计算失败", e);
        }
    }

    private void append(VehicleTask vehicleTask, String packageId, String stage, String action, int attemptNo,
                        Long offset, String result, String reason, String errorCode,
                        Instant sourceOccurredAt, Instant receivedAt) {
        String retryEventId = eventId(vehicleTask.getId().getValue(), packageId, stage, action, attemptNo,
                offset, trigger(packageId, action, attemptNo));
        TaskVehicleRetryLog entity = TaskVehicleRetryLog.builder()
                .taskId(vehicleTask.getTaskId())
                .vin(vehicleTask.getVin())
                .retryEventId(retryEventId)
                .vehicleTaskId(vehicleTask.getId().getValue())
                .stage(stage)
                .action(action)
                .attemptNo(attemptNo)
                .offset(offset)
                .result(result)
                .reason(reason)
                .errorCode(errorCode)
                .retriedAt(receivedAt == null ? Instant.now() : receivedAt)
                .sourceOccurredAt(sourceOccurredAt)
                .receivedAt(receivedAt == null ? Instant.now() : receivedAt)
                .description("CR-026 业务重试轨迹")
                .build();
        try {
            retryLogRepository.appendIdempotent(entity);
        } catch (RuntimeException e) {
            // 轨迹写入失败不得掩盖主业务事实：冲突属于确定性业务错误，其余只降级记录并计数
            metrics.increment("retry_log_write_failure");
            throw e;
        }
    }

    /**
     * 业务触发身份：下载按 (package, action, attempt) 唯一，安装按执行尝试唯一；
     * 技术重投复用同一身份，因此不会追加新行。
     */
    private static String trigger(String packageId, String action, int attemptNo) {
        return packageId == null ? action + "#" + attemptNo : packageId + "#" + action + "#" + attemptNo;
    }
}
