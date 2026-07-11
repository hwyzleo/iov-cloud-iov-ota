package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * tb_upgrade_package 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_upgrade_package")
public class UpgradePackagePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 升级活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 软件内部版本ID
     */
    @TableField("software_build_version_id")
    private Long softwareBuildVersionId;

    /**
     * 包类型：FULL / DELTA
     */
    @TableField("package_type")
    private String packageType;

    /**
     * 基础软件零件号
     */
    @TableField("base_software_pn")
    private String baseSoftwarePn;

    /**
     * 基础软件版本
     */
    @TableField("base_software_ver")
    private String baseSoftwareVer;

    /**
     * 目标软件零件号
     */
    @TableField("target_software_pn")
    private String targetSoftwarePn;

    /**
     * 目标软件版本
     */
    @TableField("target_software_ver")
    private String targetSoftwareVer;

    /**
     * 升级包URL
     */
    @TableField("package_url")
    private String packageUrl;

    /**
     * 升级包大小
     */
    @TableField("package_size")
    private Long packageSize;

    /**
     * 升级包MD5
     */
    @TableField("package_md5")
    private String packageMd5;

    /**
     * 升级包SHA256
     */
    @TableField("package_sha256")
    private String packageSha256;

    /**
     * 签名密钥引用
     */
    @TableField("sign_ref")
    private String signRef;

    /**
     * 加密密钥引用
     */
    @TableField("encrypt_ref")
    private String encryptRef;

    /**
     * 构建状态
     */
    @TableField("build_state")
    private String buildState;

    /**
     * 测试状态
     */
    @TableField("test_state")
    private String testState;

    /**
     * 是否OTA包
     */
    @TableField("ota")
    private Integer ota;

}
