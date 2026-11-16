package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import net.hwyz.iov.cloud.framework.common.exception.BaseException;

import java.util.List;

/**
 * 软件包元数据不完整（IOV-OTA-DSN-CR-026 §7.5、错误码文档）。
 *
 * <p>错误码段：403000～403999（IOV-OTA 项目段）。包大小、SHA-256、签名、证书引用或
 * 物理对象任一缺失/非法即 fail-closed，禁止登记、修改、发布或下发。
 *
 * @author hwyz_leo
 */
public class PackageMetadataIncompleteException extends BaseException {

    /** 403014：包大小、SHA-256、签名、证书引用或物理对象缺失/非法。 */
    public static final int ERROR_CODE_INCOMPLETE = 403014;

    private final List<String> reasons;

    public PackageMetadataIncompleteException(String packageKey, List<String> reasons) {
        super(ERROR_CODE_INCOMPLETE, "软件包[" + packageKey + "]元数据不完整：" + String.join(",", reasons));
        this.reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public List<String> getReasons() {
        return reasons;
    }
}
