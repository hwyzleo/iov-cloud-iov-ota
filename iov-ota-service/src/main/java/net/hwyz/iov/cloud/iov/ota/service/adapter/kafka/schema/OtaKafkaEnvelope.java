package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 业务 Envelope（CR-013 §3）
 *
 * <p>车云接入层将车端 MQTT 消息桥接为 Kafka value 后交予 IOV-OTA 的统一信封格式。
 * messageType + schemaVersion 决定 payload schema；不建立 v1/v2 双轨路由。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtaKafkaEnvelope {

    /** 物理消息ID（一次物理消息）；业务幂等不得只依赖它 */
    private String messageId;

    /** 业务消息类型，如 ota.task.detect.requested */
    private String messageType;

    /** schema 版本 */
    private Integer schemaVersion;

    /** 消息时间（ISO-8601） */
    private String timestamp;

    /** 设备ID */
    private String deviceId;

    /** 车架号 */
    private String vin;

    /** 关联 ID（请求与异步结果关联） */
    private String correlationId;

    /** 因果 ID（直接触发当前消息的上游消息） */
    private String causationId;

    /** 链路追踪 ID */
    private String traceId;

    /** 业务幂等键（可选，与 payloadDigest 共同用于幂等） */
    private String idempotencyKey;

    /** payload 摘要，如 sha256:xxx，用于检测同业务唯一键不同内容的冲突 */
    private String payloadDigest;

    /** 业务负载 */
    private JsonNode payload;
}
