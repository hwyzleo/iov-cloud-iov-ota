package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

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
     * 软件包类型
     */
    private String packageType;

    /**
     * 软件包来源
     */
    private String packageSource;

    /**
     * 基础软件零件号（仅DELTA必填）
     */
    private String baseSoftwarePn;

    /**
     * 基础软件版本（仅DELTA，与baseSoftwarePn成对）
     */
    private String baseSoftwareVer;

    /**
     * 软件包适配级别：1-LE,2-GE,3-EQ（DELTA必填，FULL默认LE）
     */
    private Integer packageAdaptiveLevel;

    /**
     * 适配的硬件总成零件号
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
     * 制品可用性状态：1-可用,2-停用,3-吊销,4-退役
     */
    private Integer packageState;

    /**
     * 创建时间
     */
    private Date createTime;

}
