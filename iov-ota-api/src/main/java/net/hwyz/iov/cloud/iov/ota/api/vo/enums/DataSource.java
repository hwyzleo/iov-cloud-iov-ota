package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 数据来源枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum DataSource {

    BOM("BOM", 1);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static DataSource valOf(Integer val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(DataSource.values())
                .filter(dataSource -> dataSource.value == val)
                .findFirst()
                .orElse(null);
    }

}
