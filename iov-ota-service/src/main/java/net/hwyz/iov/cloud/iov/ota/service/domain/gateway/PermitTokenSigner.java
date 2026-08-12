package net.hwyz.iov.cloud.iov.ota.service.domain.gateway;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

import java.time.Instant;

/**
 * 安装许可令牌签发网关（CR-012 §5.5）
 *
 * <p>TODO: 接入 KMS/HSM 进行令牌签发。当前为本地占位实现。
 * 领域层通过此接口与密钥管理基础设施解耦。
 *
 * @author hwyz_leo
 */
public interface PermitTokenSigner {

    /**
     * 签发安装许可令牌。
     *
     * @param executionId   执行ID
     * @param vehicleTaskId 车辆任务ID
     * @param attemptNo     尝试序号
     * @param validUntil    许可有效期（仅限制进入 INSTALL_STARTED）
     * @return 签发的许可令牌
     */
    PermitToken sign(ExecutionId executionId, VehicleTaskId vehicleTaskId, int attemptNo, Instant validUntil);
}
