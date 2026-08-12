package net.hwyz.iov.cloud.iov.ota.service.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.PermitTokenSigner;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * 安装许可令牌签发本地占位实现（CR-012 §5.5）
 *
 * <p>TODO: 接入 KMS/HSM 进行令牌签发。当前使用本地 UUID + HMAC 占位。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class LocalPermitTokenSigner implements PermitTokenSigner {

    @Override
    public PermitToken sign(ExecutionId executionId, VehicleTaskId vehicleTaskId, int attemptNo, Instant validUntil) {
        // TODO: 接入 KMS/HSM，使用非对称签名签发许可令牌
        String token = "permit-" + UUID.randomUUID().toString().replace("-", "")
                + "-" + executionId.getValue()
                + "-" + vehicleTaskId.getValue()
                + "-" + attemptNo;
        log.debug("本地占位签发安装许可令牌，执行[{}]，车辆任务[{}]", executionId.getValue(), vehicleTaskId.getValue());
        return PermitToken.of(token, validUntil);
    }
}
