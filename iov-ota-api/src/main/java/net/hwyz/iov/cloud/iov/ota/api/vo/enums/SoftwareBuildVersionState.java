package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 软件内部版本发布工作流状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum SoftwareBuildVersionState {

    DRAFT("草稿", 1),
    TESTING("测试中", 2),
    RELEASED("已发布", 3),
    DEPRECATED("停用", 4),
    RETIRED("退役", 5);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static SoftwareBuildVersionState valOf(Integer val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(SoftwareBuildVersionState.values())
                .filter(state -> state.value == val)
                .findFirst()
                .orElse(null);
    }

}
