package net.hwyz.iov.cloud.iov.ota.service.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 软件包元数据完整性门禁（IOV-OTA-DSN-CR-026 §7.5 / US-020）。
 *
 * <p>fail-closed：包大小、SHA-256、签名、签名算法、证书引用与物理对象引用任一缺失或非法
 * 时禁止登记/修改/发布/下发；**不得**把 NULL/未知大小转换成 0 后继续下发（历史“0 字节下载
 * 成功”假象的根因）。存量不完整包只做只读完整性标记与发布阻断，不自动回填任何值。
 *
 * @author hwyz_leo
 */
public final class PackageMetadataGate {

    /** SHA-256 小写十六进制格式 */
    private static final Pattern SHA256_PATTERN = Pattern.compile("[0-9a-f]{64}");

    /** 原因：包大小缺失或非法 */
    public static final String REASON_SIZE = "PACKAGE_SIZE_MISSING_OR_INVALID";
    /** 原因：SHA-256 缺失或非法 */
    public static final String REASON_SHA256 = "PACKAGE_SHA256_MISSING_OR_INVALID";
    /** 原因：签名缺失 */
    public static final String REASON_SIGNATURE = "PACKAGE_SIGNATURE_MISSING";
    /** 原因：签名算法缺失 */
    public static final String REASON_SIGN_ALGO = "PACKAGE_SIGN_ALGO_MISSING";
    /** 原因：签名证书引用缺失 */
    public static final String REASON_SIGNER_CERT = "PACKAGE_SIGNER_CERT_MISSING";
    /** 原因：物理对象引用缺失 */
    public static final String REASON_OBJECT = "PACKAGE_OBJECT_MISSING";

    private PackageMetadataGate() {
    }

    /**
     * 计算不完整原因（空列表表示完整）。
     *
     * @param packageSize      包字节数
     * @param packageSha256    SHA-256（小写十六进制）
     * @param packageSignature 数字签名
     * @param signAlgo         签名算法
     * @param signerCertId     签名证书引用
     * @param packageUrl       物理对象引用（对象键/URL）
     * @return 不完整原因列表
     */
    public static List<String> incompleteReasons(Long packageSize, String packageSha256,
                                                 String packageSignature, String signAlgo,
                                                 String signerCertId, String packageUrl) {
        List<String> reasons = new ArrayList<>();
        if (packageSize == null || packageSize <= 0) {
            reasons.add(REASON_SIZE);
        }
        String sha256 = packageSha256 == null ? null : packageSha256.trim().toLowerCase(Locale.ROOT);
        if (sha256 == null || !SHA256_PATTERN.matcher(sha256).matches()) {
            reasons.add(REASON_SHA256);
        }
        if (isBlank(packageSignature)) {
            reasons.add(REASON_SIGNATURE);
        }
        if (isBlank(signAlgo)) {
            reasons.add(REASON_SIGN_ALGO);
        }
        if (isBlank(signerCertId)) {
            reasons.add(REASON_SIGNER_CERT);
        }
        if (isBlank(packageUrl)) {
            reasons.add(REASON_OBJECT);
        }
        return reasons;
    }

    /**
     * 是否完整。
     */
    public static boolean isComplete(Long packageSize, String packageSha256, String packageSignature,
                                     String signAlgo, String signerCertId, String packageUrl) {
        return incompleteReasons(packageSize, packageSha256, packageSignature, signAlgo,
                signerCertId, packageUrl).isEmpty();
    }

    /**
     * 原因摘要（稳定、可读、截断到列宽内）。
     */
    public static String summary(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        String joined = String.join(",", reasons);
        return joined.length() > 255 ? joined.substring(0, 255) : joined;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
