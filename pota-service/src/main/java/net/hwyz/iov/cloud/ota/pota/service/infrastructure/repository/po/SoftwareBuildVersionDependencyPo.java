package net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po;

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
 * 软件内部版本依赖关系表 数据对象
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
@TableName("tb_software_build_version_dependency")
public class SoftwareBuildVersionDependencyPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 软件内部版本ID
     */
    @TableField("software_build_version_id")
    private Long softwareBuildVersionId;

    /**
     * 依赖软件内部版本ID
     */
    @TableField("dependency_software_build_version_id")
    private Long dependencySoftwareBuildVersionId;

    /**
     * 适配级别：1-版本及以下，2-版本及以上，3-与版本一致
     */
    @TableField("adaptive_level")
    private Integer adaptiveLevel;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;
}
