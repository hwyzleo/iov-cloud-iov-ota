package net.hwyz.iov.cloud.ota.pota.api.contract.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 配置字类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum ConfigWordType {

    FIXED("固定配置字", 1),
    SOFTWARE_PART_VERSION("软件零件版本配置字", 2);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static ConfigWordType valOf(Integer val) {
        return Arrays.stream(ConfigWordType.values())
                .filter(configWordType -> configWordType.value == val)
                .findFirst()
                .orElse(null);
    }

}
