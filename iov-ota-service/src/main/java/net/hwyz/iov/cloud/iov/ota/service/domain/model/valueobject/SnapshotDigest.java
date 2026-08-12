package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;

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
     */
    public boolean matches(SnapshotDigest other) {
        if (other == null) {
            return false;
        }
        return this.value.equals(other.value);
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
