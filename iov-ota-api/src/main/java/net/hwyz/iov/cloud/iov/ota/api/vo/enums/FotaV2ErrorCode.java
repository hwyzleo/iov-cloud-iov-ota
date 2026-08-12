package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * CCP FOTA v2 错误码域（CR-012 §7）
 *
 * <p>错误码域：OTA-AUTH-*、OTA-TASK-*、OTA-WINDOW-*、OTA-PACKAGE-*、
 * OTA-INSTALL-*、OTA-EVENT-*、OTA-CONTROL-*、OTA-LOG-*。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum FotaV2ErrorCode implements ErrorCode {

    // ===== OTA-AUTH-* 认证与公共协议 =====
    AUTH_DEVICE_BIND_FAIL("OTA-AUTH-001", "设备证书/Token 与 VIN、deviceId 绑定校验失败"),
    AUTH_REPLAY_DETECTED("OTA-AUTH-002", "检测到重放请求（timestamp + nonce）"),
    AUTH_CLOCK_DEVIATION("OTA-AUTH-003", "请求时间戳时钟偏差超限"),
    AUTH_PROTOCOL_UNSUPPORTED("OTA-AUTH-004", "协议版本不支持"),
    AUTH_IDEMPOTENCY_MISSING("OTA-AUTH-005", "写操作缺少幂等键"),
    AUTH_IDEMPOTENCY_CONFLICT("OTA-AUTH-006", "相同幂等键但请求摘要不同"),

    // ===== OTA-TASK-* 任务 =====
    TASK_NOT_FOUND("OTA-TASK-001", "任务不存在"),
    TASK_VEHICLE_NOT_FOUND("OTA-TASK-002", "车辆任务不存在"),
    TASK_SNAPSHOT_CHANGED("OTA-TASK-003", "任务快照已变化"),

    // ===== OTA-WINDOW-* 时间窗口 =====
    WINDOW_NOT_STARTED("OTA-WINDOW-001", "尚未到达安装执行窗口"),
    WINDOW_EXPIRED("OTA-WINDOW-002", "已超过安装执行窗口"),
    WINDOW_PAUSED("OTA-WINDOW-003", "任务已暂停"),

    // ===== OTA-PACKAGE-* 包 =====
    PACKAGE_NOT_READY("OTA-PACKAGE-001", "包未就绪"),
    PACKAGE_DIGEST_MISMATCH("OTA-PACKAGE-002", "包摘要不匹配"),
    PACKAGE_STAGE_INVALID("OTA-PACKAGE-003", "包阶段结果无效"),

    // ===== OTA-INSTALL-* 安装 =====
    INSTALL_EXECUTION_ACTIVE("OTA-INSTALL-001", "已存在活动执行"),
    INSTALL_PERMIT_EXPIRED("OTA-INSTALL-002", "安装许可已过期"),
    INSTALL_CONSENT_INVALID("OTA-INSTALL-003", "授权无效"),
    INSTALL_MANIFEST_MISMATCH("OTA-INSTALL-004", "包清单摘要校验失败"),
    INSTALL_NOT_READY("OTA-INSTALL-005", "车辆任务未就绪可安装"),

    // ===== OTA-EVENT-* 事件 =====
    EVENT_CONFLICT("OTA-EVENT-001", "事件摘要冲突"),
    EVENT_SEQUENCE_GAP("OTA-EVENT-002", "事件序号缺失"),

    // ===== OTA-CONTROL-* 控制 =====
    CONTROL_REVISION_CONFLICT("OTA-CONTROL-001", "控制版本冲突"),

    // ===== OTA-LOG-* 日志 =====
    LOG_AUTH_INVALID("OTA-LOG-001", "日志上传凭证无效"),
    LOG_DIGEST_MISMATCH("OTA-LOG-002", "日志摘要不匹配"),

    // ===== 通用 =====
    INTERNAL_ERROR("OTA-COMMON-001", "系统内部错误");

    private final String code;
    private final String message;
}
