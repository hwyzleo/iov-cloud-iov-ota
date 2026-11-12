package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import vehicle.common.v1.Envelope.MessageKind;

/**
 * FOTA payload 处理器（CR-014 §4.2/§5）
 *
 * <p>每个 vehicle.fota.v1.&lt;MessageName&gt; 对应一个 generated parser 与一个 handler；
 * Router 只依据 payload_type + message_kind 解析，禁止 switch (ota.* string)、反射扫描或手写 allowlist。
 *
 * @param <T> 该 payload_type 对应的 generated 强类型
 * @author hwyz_leo
 */
public interface FotaPayloadHandler<T extends Message> {

    /** 全限定 payload_type，如 vehicle.fota.v1.TaskCheckRequest */
    String payloadType();

    /** 上行期望的 message_kind（REQUEST 或 EVENT） */
    MessageKind messageKind();

    /** 使用同一 release 的 generated parser 解析 payload bytes */
    T parse(ByteString payloadBytes) throws InvalidProtocolBufferException;

    /** 计算业务唯一键（Inbox 幂等/冲突判定，Envelope 级 message_id+sha256 之上） */
    String businessKey(FotaMessageMetadata metadata, T payload);

    /**
     * 执行业务（同事务），并追加下行 RESPONSE/EVENT Envelope 到 Outbox。
     *
     * @return 结果 Outbox 消息 ID（可为 null）
     */
    Long handle(FotaMessageMetadata metadata, T payload);

    /**
     * 同 message_id 不同 Envelope 摘要冲突等契约冲突时，生产可表达的业务拒绝事件。
     *
     * @return 拒绝 Outbox 消息 ID
     */
    Long handleConflict(FotaMessageMetadata metadata, T payload, String reason);

    /**
     * 业务处理抛异常时的**可表达拒绝响应**（协议加固）。
     *
     * <p>默认返回 null 表示该类 payload 未提供拒绝响应，异常继续上抛由消费方按可恢复处理；
     * 支持的 handler 必须返回 Outbox 消息 ID，让车端拿到带错误状态的 RESPONSE，
     * 而不是只能等到传输超时（实车 2026-11-12：DownloadGrant 业务异常静默 → 车端 2.5s 超时）。</p>
     *
     * @param reasonCode 稳定错误码（大写蛇形，如 BUSINESS_ERROR）
     * @param reason     可定位描述
     * @return 拒绝 Outbox 消息 ID；null 表示不支持
     */
    default Long handleFailure(FotaMessageMetadata metadata, T payload,
                               String reasonCode, String reason) {
        return null;
    }
}
