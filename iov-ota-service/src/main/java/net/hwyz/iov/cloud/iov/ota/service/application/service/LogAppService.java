package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.LogAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 日志应用服务（CR-012 §5.8、US-082）
 *
 * <p>日志上传拆为短期凭证申请和上传结果确认，校验采集范围、脱敏版本、对象摘要与任务关联。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogAppService {

    private final VehicleTaskRepository vehicleTaskRepository;

    /**
     * 申请日志上传凭证。
     *
     * @param cmd 日志授权命令
     * @return 日志授权结果
     */
    public LogAuthResult authorizeLog(LogAuthCmd cmd) {
        log.info("车辆[{}]申请日志上传凭证，车辆任务[{}]", cmd.getVin(), cmd.getVehicleTaskId());

        // TODO: 接入 OSS 生成短期对象键和上传凭证
        String logRequestId = "log-" + UUID.randomUUID().toString().replace("-", "");
        String objectKey = "ota-logs/" + cmd.getVin() + "/" + cmd.getVehicleTaskId() + "/" + logRequestId;
        String credentialToken = "log-cred-" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(30));

        return LogAuthResult.builder()
                .logRequestId(logRequestId)
                .objectKey(objectKey)
                .credentialToken(credentialToken)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 提交日志上传结果。
     *
     * @param cmd 日志结果命令
     */
    public void submitLogResult(LogResultCmd cmd) {
        log.info("车辆[{}]提交日志上传结果，车辆任务[{}]，结果[{}]",
                cmd.getVin(), cmd.getVehicleTaskId(), cmd.getUploadResult());

        // 校验车辆任务存在
        vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId()))
                .orElseThrow(() -> new IllegalStateException("车辆任务[" + cmd.getVehicleTaskId() + "]不存在"));

        // TODO: 校验日志摘要后关联到 VehicleTask/Execution
        // TODO: 持久化日志记录到 tb_upgrade_log（重构后的字段）
    }
}
