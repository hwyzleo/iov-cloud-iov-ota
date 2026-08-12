package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ECU 清单握手处置枚举类（CR-012 §5.1、§7）
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum InventoryDisposition {

    ACCEPTED("已接受"),
    FULL_REQUIRED("需重发完整清单"),
    REVISION_CONFLICT("版本冲突"),
    DIGEST_MISMATCH("摘要不匹配"),
    ALGORITHM_UNSUPPORTED("算法不支持");

    public final String label;
    public final String value = name();

    public static InventoryDisposition valOf(String val) {
        if (val == null) {
            return null;
        }
        for (InventoryDisposition disposition : values()) {
            if (disposition.value.equals(val)) {
                return disposition;
            }
        }
        return null;
    }
}
