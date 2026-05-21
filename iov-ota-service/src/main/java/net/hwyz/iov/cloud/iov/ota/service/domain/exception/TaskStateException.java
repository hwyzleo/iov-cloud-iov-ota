package net.hwyz.iov.cloud.iov.ota.service.domain.exception;

public class TaskStateException extends RuntimeException {
    
    public TaskStateException(String message) {
        super(message);
    }
    
    public TaskStateException(String message, Throwable cause) {
        super(message, cause);
    }
}