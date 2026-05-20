package net.hwyz.iov.cloud.iov.ota.service.domain.activity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.contract.enums.AdaptiveLevel;
import net.hwyz.iov.cloud.ota.pota.api.contract.enums.SoftwarePackageType;

import java.util.Date;

/**
 * 软件包信息值对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwarePackageVo {

    /**
     * 主键
     */
    private Long id;

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
     * 软件包名称
     */
    private String packageName;

    /**
     * 软件包代码
     */
    private String packageCode;

    /**
     * 软件包URL
     */
    private String packageUrl;

    /**
     * 软件包大小（Byte）
     */
    private Long packageSize;

    /**
     * 软件包MD5
     */
    private String packageMd5;

    /**
     * 软件包说明
     */
    private String packageDesc;

    /**
     * 软件包类型
     */
    private SoftwarePackageType packageType;

    /**
     * 软件包来源：1-BOM，2-OTA
     */
    private Integer packageSource;

    /**
     * 基础软件零件号
     */
    private String baseSoftwarePn;

    /**
     * 基础软件版本
     */
    private String baseSoftwareVer;

    /**
     * 软件包适配级别：1-基础版本及以下，2-基础版本及以上，3-与基础版本一致
     */
    private AdaptiveLevel packageAdaptiveLevel;

    /**
     * 适配的总成软件零件号
     */
    private String adaptiveSoftwarePn;

    /**
     * 发布日期
     */
    private Date publishDate;

    /**
     * 预计升级时间（分钟）
     */
    private Integer estimatedInstallTime;

    /**
     * 是否是OTA包
     */
    private Boolean ota;

    /**
     * 是否匹配
     */
    private Boolean match;

    /**
     * 创建时间
     */
    private Date createTime;

}
