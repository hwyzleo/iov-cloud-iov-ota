package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 升级模式枚举类
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum UpgradeMode {

    NORMAL(1, "普通模式"),
    FORCED(2, "强制模式"),
    SCHEDULED(3, "预约模式"),
    REMOTE(4, "远程模式"),
    FACTORY(5, "工厂模式");

    private final int value;
    private final String label;

    public static UpgradeMode valOf(Integer val) {
        return Arrays.stream(UpgradeMode.values())
                .filter(upgradeMode -> upgradeMode.value == val)
                .findFirst()
                .orElse(null);
    }

}
