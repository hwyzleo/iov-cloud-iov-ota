package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.ccp;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.contract.FotaV2Response;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.FotaV2ErrorCode;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.FotaV2Exception;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleTaskStateException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * CCP FOTA v2 全局异常处理器（CR-012 §4、§7）
 *
 * <p>将 FotaV2Exception 及领域异常映射为 FotaV2Response 错误码。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestControllerAdvice(assignableTypes = CcpFotaV2Controller.class)
public class CcpFotaV2ExceptionHandler {

    @ExceptionHandler(FotaV2Exception.class)
    public FotaV2Response<Void> handleFotaV2Exception(FotaV2Exception e) {
        log.warn("FOTA v2 协议异常：{}", e.getMessage());
        return FotaV2Response.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(VehicleTaskStateException.class)
    public FotaV2Response<Void> handleVehicleTaskStateException(VehicleTaskStateException e) {
        log.warn("车辆任务状态异常：{}", e.getMessage());
        return FotaV2Response.fail(FotaV2ErrorCode.TASK_VEHICLE_NOT_FOUND.getCode(), e.getMessage());
    }

    @ExceptionHandler(ExecutionStateException.class)
    public FotaV2Response<Void> handleExecutionStateException(ExecutionStateException e) {
        log.warn("执行状态异常：{}", e.getMessage());
        return FotaV2Response.fail(FotaV2ErrorCode.INSTALL_EXECUTION_ACTIVE.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public FotaV2Response<Void> handleException(Exception e) {
        log.error("FOTA v2 内部异常", e);
        return FotaV2Response.fail(FotaV2ErrorCode.INTERNAL_ERROR.getCode(), e.getMessage());
    }
}
