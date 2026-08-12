package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 安装事件处置枚举类（CR-012 §5.6、§7）
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum EventDisposition {

    ACCEPTED("已接受"),
    DUPLICATE("重复"),
    BUFFERED("已暂存"),
    REJECTED("已拒绝"),
    CONFLICT("摘要冲突");

    public final String label;
    public final String value = name();

    public static EventDisposition valOf(String val) {
        if (val == null) {
            return null;
        }
        for (EventDisposition disposition : values()) {
            if (disposition.value.equals(val)) {
                return disposition;
            }
        }
        return null;
    }
}
