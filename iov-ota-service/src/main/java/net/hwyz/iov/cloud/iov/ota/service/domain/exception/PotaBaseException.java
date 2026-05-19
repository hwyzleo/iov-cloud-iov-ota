package net.hwyz.iov.cloud.iov.ota.service.domain.exception;


import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * OTA零件服务基础异常
 *
 * @author hwyz_leo
 */
public class PotaBaseException extends BaseException {

    private static final int ERROR_CODE = 402000;
    protected static final int ERROR_CODE_SOFTWARE_BUILD_VERSION_NOT_EXIST = 401001;
    protected static final int ERROR_CODE_PART_NOT_EXIST = 401002;
    protected static final int ERROR_CODE_COMPATIBLE_PN_NOT_EXIST = 401003;
    protected static final int ERROR_CODE_FIXED_CONFIG_WORD_NOT_EXIST = 401004;

    public PotaBaseException(String message) {
        super(ERROR_CODE, message);
    }

    public PotaBaseException(int errorCode) {
        super(errorCode);
    }

    public PotaBaseException(int errorCode, String message) {
        super(errorCode, message);
    }

}
