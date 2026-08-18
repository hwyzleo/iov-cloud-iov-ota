package net.hwyz.iov.cloud.iov.ota.service.domain.exception;

/**
 * 车辆投影回源可重试异常（CR-015 §4.2）
 * <p>VMD 服务不可用时抛出；不得缓存为 NOT_FOUND，需受控重试/隔离。</p>
 *
 * @author hwyz_leo
 */
public class RetryableProjectionException extends RuntimeException {

    public RetryableProjectionException(String message) {
        super(message);
    }

    public RetryableProjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
