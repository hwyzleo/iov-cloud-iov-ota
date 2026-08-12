package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema;

import java.util.Arrays;

/**
 * OTA 车云消息类型（CR-013 §4）
 *
 * <p>IOV-OTA 消费的上行事件与生产的下行结果/命令/回执消息类型。
 *
 * @author hwyz_leo
 */
public enum OtaMessageType {

    // ==================== IOV-OTA 消费（上行） ====================
    TASK_DETECT_REQUESTED("ota.task.detect.requested"),
    TASK_DISPOSITION_REPORTED("ota.task.disposition.reported"),
    CONSENT_REPORTED("ota.consent.reported"),
    PACKAGE_DOWNLOAD_AUTH_REQUESTED("ota.package.download-authorization.requested"),
    PACKAGE_STAGE_RESULT_REPORTED("ota.package.stage-result.reported"),
    EXECUTION_PERMIT_REQUESTED("ota.execution.permit.requested"),
    EXECUTION_EVENT_REPORTED("ota.execution.event.reported"),
    EXECUTION_CONTROL_ACK_REPORTED("ota.execution.control-ack.reported"),
    EXECUTION_RESULT_REPORTED("ota.execution.result.reported"),
    LOG_UPLOAD_AUTH_REQUESTED("ota.log.upload-authorization.requested"),
    LOG_UPLOAD_RESULT_REPORTED("ota.log.upload-result.reported"),
    RECOVERY_REQUESTED("ota.recovery.requested"),
    POLICY_SYNC_REQUESTED("ota.policy.sync.requested"),

    // ==================== IOV-OTA 生产（下行） ====================
    TASK_DETECTED("ota.task.detected"),
    TASK_DETECT_REJECTED("ota.task.detect.rejected"),
    TASK_DISPOSITION_ACKNOWLEDGED("ota.task.disposition.acknowledged"),
    CONSENT_RECORDED("ota.consent.recorded"),
    CONSENT_REJECTED("ota.consent.rejected"),
    PACKAGE_DOWNLOAD_AUTHORIZED("ota.package.download-authorized"),
    PACKAGE_DOWNLOAD_DENIED("ota.package.download-denied"),
    PACKAGE_STAGE_RESULT_ACKNOWLEDGED("ota.package.stage-result.acknowledged"),
    EXECUTION_PERMITTED("ota.execution.permitted"),
    EXECUTION_PERMIT_DENIED("ota.execution.permit-denied"),
    EXECUTION_EVENT_ACKNOWLEDGED("ota.execution.event.acknowledged"),
    EXECUTION_CONTROL_ISSUED("ota.execution.control.issued"),
    EXECUTION_CONTROL_ACK_ACKNOWLEDGED("ota.execution.control-ack.acknowledged"),
    EXECUTION_RESULT_ACKNOWLEDGED("ota.execution.result.acknowledged"),
    EXECUTION_RESULT_REJECTED("ota.execution.result.rejected"),
    LOG_UPLOAD_AUTHORIZED("ota.log.upload-authorized"),
    LOG_UPLOAD_RESULT_ACKNOWLEDGED("ota.log.upload-result.acknowledged"),
    RECOVERY_RESOLVED("ota.recovery.resolved"),
    POLICY_SYNCED("ota.policy.synced"),
    POLICY_CONFLICTED("ota.policy.conflicted");

    private final String value;

    OtaMessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OtaMessageType fromValue(String value) {
        return Arrays.stream(values())
                .filter(t -> t.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
