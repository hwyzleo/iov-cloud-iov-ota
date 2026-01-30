package net.hwyz.iov.cloud.ota.pota.api.contract.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 数据类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum DataType {

    BASELINE("基线", 1),
    SWO("售后变更", 2);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static DataType valOf(Integer val) {
        return Arrays.stream(DataType.values())
                .filter(dataType -> dataType.value == val)
                .findFirst()
                .orElse(null);
    }

}
