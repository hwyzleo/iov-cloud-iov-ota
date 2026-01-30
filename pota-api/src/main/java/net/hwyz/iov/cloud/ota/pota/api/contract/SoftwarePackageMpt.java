package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.web.domain.BaseRequest;

import java.util.Date;

/**
 * 管理后台软件包信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SoftwarePackageMpt extends BaseRequest {

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
    private String packageType;

    /**
     * 软件包来源
     */
    private String packageSource;

    /**
     * 基础软件零件号
     */
    private String baseSoftwarePn;

    /**
     * 软件包适配级别：1-基础版本及以下，2-基础版本及以上，3-与基础版本一致
     */
    private Integer packageAdaptiveLevel;

    /**
     * 适配的总成零件号
     */
    private String adaptiveAssemblyPn;

    /**
     * 发布日期
     */
    private Date releaseDate;

    /**
     * 预计升级时间（分钟）
     */
    private Integer estimatedInstallTime;

    /**
     * 是否是OTA包
     */
    private Boolean ota;

    /**
     * 创建时间
     */
    private Date createTime;

}
