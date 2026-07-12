package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 软件内部版本软硬件适配矩阵 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_software_build_version_adaptation")
public class SoftwareBuildVersionAdaptationPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("sbv_id")
    private Long sbvId;

    @TableField("hardware_assembly_pn")
    private String hardwareAssemblyPn;

    @TableField("hardware_ver")
    private String hardwareVer;

    @TableField("adaptive_level")
    private Integer adaptiveLevel;

    @TableField("sort")
    private Integer sort;
}
