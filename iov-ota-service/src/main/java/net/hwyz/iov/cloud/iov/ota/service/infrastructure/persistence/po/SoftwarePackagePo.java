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
     * 软件包MD5
     */
    @TableField("package_md5")
    private String packageMd5;

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
     * 基础软件零件号
     */
    @TableField("base_software_pn")
    private String baseSoftwarePn;

    /**
     * 软件包适配级别：1-基础版本及以下，2-基础版本及以上，3-与基础版本一致
     */
    @TableField("package_adaptive_level")
    private Integer packageAdaptiveLevel;

    /**
     * 适配的总成零件号
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
}
