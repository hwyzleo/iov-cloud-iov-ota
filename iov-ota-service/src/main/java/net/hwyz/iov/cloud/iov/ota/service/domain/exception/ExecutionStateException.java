package net.hwyz.iov.cloud.iov.ota.service.domain.exception;

/**
 * 安装执行状态异常（CR-012 §2.3）
 *
 * @author hwyz_leo
 */
public class ExecutionStateException extends RuntimeException {

    public ExecutionStateException(String message) {
        super(message);
    }

    public ExecutionStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
