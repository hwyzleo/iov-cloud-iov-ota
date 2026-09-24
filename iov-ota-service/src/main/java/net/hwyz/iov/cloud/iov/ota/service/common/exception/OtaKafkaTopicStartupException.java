package net.hwyz.iov.cloud.iov.ota.service.common.exception;

/**
 * Kafka Topic 启动校验异常（CR-020 §4.24.3 / RD-020-3）
 *
 * <p>消费 Topic 缺失、生产 Topic 配置漂移或旧名称残留且校验模式为 FAIL_FAST 时抛出，
 * 由 ApplicationReadyEvent 监听器传播阻断应用启动；消费 Topic 绝不在此创建。
 *
 * @author hwyz_leo
 */
public class OtaKafkaTopicStartupException extends RuntimeException {

    public OtaKafkaTopicStartupException(String message) {
        super(message);
    }

    public OtaKafkaTopicStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
