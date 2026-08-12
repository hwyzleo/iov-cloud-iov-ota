package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * 安装许可令牌值对象（CR-012 §5.5）
 *
 * <p>permitToken 由云端在创建 Execution 时签发，冻结安装许可策略。
 * validUntil 仅限制进入 INSTALL_STARTED；已开始后不得因许可自然过期直接中断。
 *
 * @author hwyz_leo
 */
@Getter
public class PermitToken {

    private final String token;
    private final Instant signedAt;
    private final Instant validUntil;

    public static PermitToken of(String token, Instant validUntil) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("PermitToken must not be empty");
        }
        return new PermitToken(token, Instant.now(), validUntil);
    }

    public PermitToken(String token, Instant signedAt, Instant validUntil) {
        this.token = token;
        this.signedAt = signedAt;
        this.validUntil = validUntil;
    }

    /**
     * 许可是否仍可进入安装（尚未开始且未过期）。
     */
    public boolean isPermitValid() {
        return validUntil == null || Instant.now().isBefore(validUntil);
    }

    /**
     * 判断令牌值是否一致（用于幂等校验）。
     */
    public boolean matches(String otherToken) {
        return otherToken != null && this.token.equals(otherToken);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermitToken that = (PermitToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }

    @Override
    public String toString() {
        return "PermitToken{token='" + token + "', validUntil=" + validUntil + '}';
    }
}
