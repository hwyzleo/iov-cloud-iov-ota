package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 开放平台BOM配置字依赖
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomConfigWordDependencyOapi {

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 软件零件号
     */
    private String softwarePn;

    /**
     * 软件零件版本
     */
    private String softwarePartVer;

    /**
     * 配置字版本
     */
    private String version;

    /**
     * 配置字起始字节
     */
    private Integer startByte;

    /**
     * 配置字起始位
     */
    private Integer startBit;

    /**
     * 配置字值
     */
    private String value;

    /**
     * 适用规则
     */
    private String rule;

    /**
     * 是否默认
     */
    private Boolean isDefault;

    /**
     * 是否预留
     */
    private Boolean isReserve;

    /**
     * 依赖描述
     */
    private String description;

    /**
     * 更新时间
     */
    private Date modifyTime;

}
