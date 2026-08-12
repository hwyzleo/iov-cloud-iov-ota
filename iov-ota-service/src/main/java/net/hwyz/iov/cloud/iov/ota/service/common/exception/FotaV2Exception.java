package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.FotaV2ErrorCode;

/**
 * CCP FOTA v2 协议异常（CR-012 §4、§7）
 *
 * <p>携带 FotaV2ErrorCode，由全局异常处理器映射为 FotaV2Response。
 *
 * @author hwyz_leo
 */
public class FotaV2Exception extends RuntimeException {

    private final FotaV2ErrorCode errorCode;

    public FotaV2Exception(FotaV2ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public FotaV2Exception(FotaV2ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FotaV2ErrorCode getErrorCode() {
        return errorCode;
    }
}
