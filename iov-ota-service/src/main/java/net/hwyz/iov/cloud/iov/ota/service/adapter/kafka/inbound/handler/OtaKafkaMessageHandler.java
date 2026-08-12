package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;

/**
 * OTA Kafka 上行消息处理器接口（CR-013 §4/§5）
 *
 * <p>处理器只负责：业务唯一键提取、业务执行（应用服务 + 写领域状态 + 追加结果 Outbox）、
 * 冲突结果生产。Envelope 校验与 Inbox 去重由入站流水线统一完成。
 *
 * @author hwyz_leo
 */
public interface OtaKafkaMessageHandler {

    /**
     * 本处理器对应的上行消息类型。
     */
    OtaMessageType messageType();

    /**
     * 计算业务唯一键（用于 Inbox 去重：同键同摘要幂等，同键异摘要冲突）。
     */
    String businessKey(OtaKafkaEnvelope envelope, JsonNode payload);

    /**
     * 执行业务（同事务），并追加下行结果/回执消息到 Outbox。
     *
     * @return 结果 Outbox 消息 ID（可为 null）
     */
    Long handle(OtaKafkaEnvelope envelope, JsonNode payload);

    /**
     * 同业务键不同摘要冲突时，生产可表达的业务拒绝事件。
     *
     * @return 拒绝 Outbox 消息 ID
     */
    Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason);
}
