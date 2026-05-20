package net.hwyz.iov.cloud.iov.ota.service.infrastructure.exception;


import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * 在线固件升级服务基础异常
 *
 * @author hwyz_leo
 */
public class FotaBaseException extends BaseException {

    private static final int ERROR_CODE = 411000;

    public FotaBaseException(String message) {
        super(ERROR_CODE, message);
    }

    public FotaBaseException(int errorCode) {
        super(errorCode);
    }

    public FotaBaseException(int errorCode, String message) {
        super(errorCode, message);
    }

}
