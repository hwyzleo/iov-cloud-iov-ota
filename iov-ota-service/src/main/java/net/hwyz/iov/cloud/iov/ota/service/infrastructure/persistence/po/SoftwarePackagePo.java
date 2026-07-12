package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 软件包信息表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-30
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_software_package")
public class SoftwarePackagePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备编码
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 软件零件号
     */
    @TableField("software_pn")
    private String softwarePn;

    /**
     * 软件包名称
     */
    @TableField("package_name")
    private String packageName;

    /**
     * 软件包代码
     */
    @TableField("package_code")
    private String packageCode;

    /**
     * 软件包URL
     */
    @TableField("package_url")
    private String packageUrl;

    /**
     * 软件包大小（Byte）
     */
    @TableField("package_size")
    private Long packageSize;

    /**
     * 软件包MD5（弱/兼容校验）
     */
    @TableField("package_md5")
    private String packageMd5;

    /**
     * 软件包SHA-256（权威完整性校验）
     */
    @TableField("package_sha256")
    private String packageSha256;

    /**
     * 软件包数字签名
     */
    @TableField("package_signature")
    private String packageSignature;

    /**
     * 签名算法（RSA/ECDSA/SM2）
     */
    @TableField("sign_algo")
    private String signAlgo;

    /**
     * 签名证书标识（引用KMS/PKI）
     */
    @TableField("signer_cert_id")
    private String signerCertId;

    /**
     * 软件包说明
     */
    @TableField("package_desc")
    private String packageDesc;

    /**
     * 软件包类型
     */
    @TableField("package_type")
    private String packageType;

    /**
     * 软件包来源
     */
    @TableField("package_source")
    private String packageSource;

    /**
     * 基础软件零件号（仅DELTA必填）
     */
    @TableField("base_software_pn")
    private String baseSoftwarePn;

    /**
     * 基础软件版本（仅DELTA，与base_software_pn成对）
     */
    @TableField("base_software_ver")
    private String baseSoftwareVer;

    /**
     * 软件包适配级别：1-LE,2-GE,3-EQ（DELTA必填，FULL默认LE）
     */
    @TableField("package_adaptive_level")
    private Integer packageAdaptiveLevel;

    /**
     * 适配的硬件总成零件号
     */
    @TableField("adaptive_assembly_pn")
    private String adaptiveAssemblyPn;

    /**
     * 发布日期
     */
    @TableField("release_date")
    private Date releaseDate;

    /**
     * 预计升级时间（分钟）
     */
    @TableField("estimated_install_time")
    private Integer estimatedInstallTime;

    /**
     * 是否是OTA包
     */
    @TableField("ota")
    private Boolean ota;

    /**
     * 制品可用性状态: ACTIVE/DEPRECATED/REVOKED/RETIRED
     */
    @TableField("package_state")
    private String packageState;
}
