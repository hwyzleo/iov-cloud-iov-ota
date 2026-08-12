package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户授权状态枚举类（CR-012 §2.2、§5.3）
 *
 * <p>accepted 与 effectiveConsentStatus 分离：receipt 是否被接受 vs 当前有效授权状态。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ConsentState {

    NOT_REQUIRED("无需授权"),
    PENDING("待授权"),
    GRANTED("已授权"),
    DENIED("已拒绝"),
    REVOKED("已撤回"),
    EXPIRED("已失效");

    public final String label;
    public final String value = name();

    public static ConsentState valOf(String val) {
        if (val == null) {
            return null;
        }
        for (ConsentState state : values()) {
            if (state.value.equals(val)) {
                return state;
            }
        }
        return null;
    }
}
