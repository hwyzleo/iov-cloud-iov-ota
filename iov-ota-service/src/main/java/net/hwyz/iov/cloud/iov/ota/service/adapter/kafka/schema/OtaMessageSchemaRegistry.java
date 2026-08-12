package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTA 消息 Schema 注册表（CR-013 §3）
 *
 * <p>messageType + schemaVersion 决定 payload schema。当前全部消息类型使用 schemaVersion=1。
 * 未知消息类型或 schemaVersion 视为不可恢复契约错误，进入 DLQ/隔离。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class OtaMessageSchemaRegistry {

    /** 当前支持的 schema 版本 */
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    /** 已注册的业务消息类型 -> schemaVersion */
    private final Map<String, Integer> registeredSchemas = new ConcurrentHashMap<>();

    public OtaMessageSchemaRegistry() {
        // 注册全部 IOV-OTA 消费的消息类型（schemaVersion=1）
        for (OtaMessageType type : OtaMessageType.values()) {
            if (isConsumed(type)) {
                registeredSchemas.put(type.getValue(), SUPPORTED_SCHEMA_VERSION);
            }
        }
    }

    /**
     * 校验消息类型与 schema 版本是否受支持。
     *
     * @return true 支持；false 不支持（契约错误）
     */
    public boolean supports(String messageType, Integer schemaVersion) {
        if (messageType == null || schemaVersion == null) {
            return false;
        }
        Integer registered = registeredSchemas.get(messageType);
        return registered != null && registered.equals(schemaVersion);
    }

    private boolean isConsumed(OtaMessageType type) {
        return switch (type) {
            case TASK_DETECT_REQUESTED, TASK_DISPOSITION_REPORTED, CONSENT_REPORTED,
                 PACKAGE_DOWNLOAD_AUTH_REQUESTED, PACKAGE_STAGE_RESULT_REPORTED,
                 EXECUTION_PERMIT_REQUESTED, EXECUTION_EVENT_REPORTED, EXECUTION_CONTROL_ACK_REPORTED,
                 EXECUTION_RESULT_REPORTED, LOG_UPLOAD_AUTH_REQUESTED, LOG_UPLOAD_RESULT_REPORTED,
                 RECOVERY_REQUESTED, POLICY_SYNC_REQUESTED -> true;
            default -> false;
        };
    }
}
