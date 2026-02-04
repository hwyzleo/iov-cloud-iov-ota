package net.hwyz.iov.cloud.ota.pota.api.contract.enums;

import lombok.AllArgsConstructor;

/**
 * 软件包类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum SoftwarePackageType {

    FULL("全量"),
    DELTA("差分");

    /**
     * 名称
     */
    public final String label;

}
