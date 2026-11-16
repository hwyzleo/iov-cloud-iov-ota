package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * 日志对象操作异常（IOV-OTA-DSN-CR-026 §7.3、错误码文档）。
 *
 * <p>错误码段：403000～403999（IOV-OTA 项目段）。确定性失败（对象缺失、对象键/大小/摘要
 * 不符、状态或幂等冲突）不得自动重试；存储暂不可用按可恢复基础设施故障重投。
 *
 * @author hwyz_leo
 */
public class LogObjectOperationException extends BaseException {

    /** 403011：日志结果确认时对象存储中不存在授权对象。 */
    public static final int ERROR_CODE_OBJECT_NOT_FOUND = 403011;
    /** 403012：对象键、实际大小或摘要与授权/上报事实不一致。 */
    public static final int ERROR_CODE_OBJECT_MISMATCH = 403012;
    /** 403013：日志申请不存在、状态不允许或同幂等键不同内容。 */
    public static final int ERROR_CODE_STATE_CONFLICT = 403013;
    /** 403016：对象存储授权、HEAD 或核验暂时不可用（可恢复）。 */
    public static final int ERROR_CODE_STORAGE_UNAVAILABLE = 403016;

    private final boolean recoverable;

    public LogObjectOperationException(int errorCode, String message) {
        this(errorCode, message, errorCode == ERROR_CODE_STORAGE_UNAVAILABLE);
    }

    public LogObjectOperationException(int errorCode, String message, boolean recoverable) {
        super(errorCode, message);
        this.recoverable = recoverable;
    }

    /**
     * 是否可按可恢复基础设施故障重投。
     */
    public boolean isRecoverable() {
        return recoverable;
    }
}
