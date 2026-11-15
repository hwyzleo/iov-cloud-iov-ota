package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;

import java.util.Locale;
import java.util.Objects;

/**
 * 快照摘要值对象（CR-012 §2.2、§3）
 *
 * <p>snapshotDigest 是 VehicleTask 快照的内容摘要，用于检测实质性变化。
 * packageManifestDigest 是包清单摘要，安装许可时需重新计算并校验。
 *
 * @author hwyz_leo
 */
@Getter
public class SnapshotDigest {

    private final String value;
    private final String algorithm;

    public static SnapshotDigest of(String value) {
        return new SnapshotDigest(value, "SHA-256");
    }

    public static SnapshotDigest of(String value, String algorithm) {
        return new SnapshotDigest(value, algorithm);
    }

    private SnapshotDigest(String value, String algorithm) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SnapshotDigest value must not be empty");
        }
        this.value = value;
        this.algorithm = algorithm != null && !algorithm.isBlank() ? algorithm : "SHA-256";
    }

    /**
     * 判断两个摘要是否实质性一致。
     *
     * <p>比较前做归一化：去除 <code>0x</code>、<code>sha256:</code>/<code>SHA-256:</code> 等前缀、
     * 去空白并统一小写。十六进制摘要的大小写与算法前缀只是书写差异，不应被判定为摘要不一致。
     *
     * @param other 参与比较的摘要
     * @return 归一化后一致返回 true；{@code other} 为 null 返回 false（无法证明一致）
     */
    public boolean matches(SnapshotDigest other) {
        if (other == null) {
            return false;
        }
        return normalize(this.value).equals(normalize(other.value));
    }

    /**
     * 摘要书写归一化，用于跨云车两侧比较。
     */
    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        int colon = normalized.lastIndexOf(':');
        if (colon >= 0) {
            normalized = normalized.substring(colon + 1);
        }
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnapshotDigest that = (SnapshotDigest) o;
        return Objects.equals(value, that.value) && Objects.equals(algorithm, that.algorithm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, algorithm);
    }

    @Override
    public String toString() {
        return "SnapshotDigest{" + algorithm + ":" + value + '}';
    }
}
