package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 开放平台BOM软件包信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomSoftwarePackageOapi {

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
     * 软件包MD5（弱/兼容校验）
     */
    private String packageMd5;

    /**
     * 软件包SHA-256（权威完整性校验）
     */
    private String packageSha256;

    /**
     * 软件包数字签名
     */
    private String packageSignature;

    /**
     * 签名算法（RSA/ECDSA/SM2）
     */
    private String signAlgo;

    /**
     * 签名证书标识（引用KMS/PKI）
     */
    private String signerCertId;

    /**
     * 软件包说明
     */
    private String packageDesc;

    /**
     * 软件包类型：1-全量，2-差分
     */
    private Integer packageType;

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
    private Integer packageAdaptionLevel;

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
     * 创建时间
     */
    private Date createTime;

}
