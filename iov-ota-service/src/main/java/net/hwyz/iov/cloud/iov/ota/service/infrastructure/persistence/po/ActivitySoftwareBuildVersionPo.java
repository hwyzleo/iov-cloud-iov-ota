package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 升级活动软件内部版本关系表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-09
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_activity_software_build_version")
public class ActivitySoftwareBuildVersionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 软件内部版本ID
     */
    @TableField("software_build_version_id")
    private Long softwareBuildVersionId;

    /**
     * 是否关键版本
     */
    @TableField("critical")
    private Boolean critical;

    /**
     * 是否支持OTA
     */
    @TableField("ota")
    private Boolean ota;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 软件版本组
     */
    @TableField("version_group")
    private Integer versionGroup;

    /**
     * 是否强制升级
     */
    @TableField("force_upgrade")
    private Boolean forceUpgrade;
}
