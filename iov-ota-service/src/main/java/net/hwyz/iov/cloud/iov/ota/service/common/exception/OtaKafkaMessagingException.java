package net.hwyz.iov.cloud.iov.ota.service.common.exception;

/**
 * OTA Kafka 消息处理异常（CR-013 §5）
 *
 * <p>区分可恢复与不可恢复错误：
 * <ul>
 *   <li>{@link Recoverable}：业务/领域瞬时可恢复错误，事务回滚、不提交 offset、由 Kafka 重投。</li>
 *   <li>{@link NonRecoverable}：契约错误（schema 未知、VIN/device 不一致、摘要冲突等），进入隔离/DLQ 并生产可表达的业务拒绝事件。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
public class OtaKafkaMessagingException extends RuntimeException {

    private final boolean recoverable;

    public OtaKafkaMessagingException(String message, boolean recoverable) {
        super(message);
        this.recoverable = recoverable;
    }

    public OtaKafkaMessagingException(String message, boolean recoverable, Throwable cause) {
        super(message, cause);
        this.recoverable = recoverable;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    /** 可恢复：事务回滚并重投 */
    public static OtaKafkaMessagingException recoverable(String message, Throwable cause) {
        return new OtaKafkaMessagingException(message, true, cause);
    }

    /** 不可恢复：进入 DLQ/隔离，并生产业务拒绝事件 */
    public static OtaKafkaMessagingException nonRecoverable(String message) {
        return new OtaKafkaMessagingException(message, false);
    }
}
