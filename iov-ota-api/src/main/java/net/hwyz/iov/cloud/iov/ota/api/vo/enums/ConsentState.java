package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户授权状态枚举类（CR-016 §3.1、§4）
 *
 * <p>VehicleTask 当前权威授权状态。accepted 与 effectiveConsentStatus 分离：
 * 有效凭据必须是 GRANTED 且绑定任务修订、条款 hash 与 scope digest。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ConsentState {

    NOT_REQUIRED("无需授权"),
    PENDING("待授权"),
    GRANTED("已授权"),
    REJECTED("已拒绝"),
    REVOKED("已撤回"),
    EXPIRED("已失效"),
    INVALIDATED("已失效（修订/条款/范围变更）");

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
