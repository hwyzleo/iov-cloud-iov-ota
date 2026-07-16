package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 安装条件类型受控词表持久化对象
 * 对应表：tb_install_condition_type
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_install_condition_type")
public class InstallConditionTypePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 条件编码
     */
    @TableField("code")
    private String code;

    /**
     * 条件名称
     */
    @TableField("name")
    private String name;

    /**
     * 单位
     */
    @TableField("unit")
    private String unit;

    /**
     * 值类型：BOOLEAN/INTEGER/DECIMAL/STRING
     */
    @TableField("value_type")
    private String valueType;

    /**
     * 默认值
     */
    @TableField("default_value")
    private String defaultValue;

    /**
     * 适用阶段（逗号分隔）：VALIDATION,CANARY,RELEASE
     */
    @TableField("applicable_phase")
    private String applicablePhase;

    /**
     * 是否必选
     */
    @TableField("mandatory")
    private Boolean mandatory;

    /**
     * 备注
     */
    @TableField("description")
    private String description;
}
