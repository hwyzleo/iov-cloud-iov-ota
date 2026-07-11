package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

/**
 * 文章类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum ArticleType {

    RELEASE_NOTE("发布说明", "RELEASE_NOTE"),
    NOTICE("升级须知", "NOTICE"),
    TERMS("用户条款", "TERMS"),
    PRIVACY("隐私声明", "PRIVACY");

    public final String label;
    public final String value;

    public static ArticleType valOf(String val) {
        for (ArticleType type : values()) {
            if (type.value.equals(val)) {
                return type;
            }
        }
        return null;
    }
}
