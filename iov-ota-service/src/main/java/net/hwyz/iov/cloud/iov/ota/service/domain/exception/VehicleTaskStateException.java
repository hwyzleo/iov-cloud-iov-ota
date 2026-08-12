package net.hwyz.iov.cloud.iov.ota.service.domain.exception;

/**
 * 车辆任务状态异常（CR-012 §2.2）
 *
 * @author hwyz_leo
 */
public class VehicleTaskStateException extends RuntimeException {

    public VehicleTaskStateException(String message) {
        super(message);
    }

    public VehicleTaskStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
