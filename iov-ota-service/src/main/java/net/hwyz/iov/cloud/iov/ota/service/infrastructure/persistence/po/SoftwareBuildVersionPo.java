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
 * 软件内部版本信息表 数据对象
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
@TableName("tb_software_build_version")
public class SoftwareBuildVersionPo extends BasePo {

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
     * 软件内部版本
     */
    @TableField("software_build_ver")
    private String softwareBuildVer;

    /**
     * 软件测试报告
     */
    @TableField("software_report")
    private String softwareReport;

    /**
     * 软件说明
     */
    @TableField("software_desc")
    private String softwareDesc;

    /**
     * 软件来源
     */
    @TableField("software_source")
    private String softwareSource;

    /**
     * 适配的总成零件号
     */
    @TableField("adaptive_assembly_pn")
    private String adaptiveAssemblyPn;

    /**
     * 适配的软件零件号
     */
    @TableField("adaptive_software_pn")
    private String adaptiveSoftwarePn;

    /**
     * 发布日期
     */
    @TableField("release_date")
    private Date releaseDate;
}
