package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 配置字字段表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-11
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_config_word_field")
public class ConfigWordFieldPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置字代码
     */
    @TableField("config_word_code")
    private String configWordCode;

    /**
     * 配置字配置文件代码
     */
    @TableField("config_word_profile_code")
    private String configWordProfileCode;

    /**
     * 字段代码
     */
    @TableField("code")
    private String code;

    /**
     * 字段名称
     */
    @TableField("name")
    private String name;

    /**
     * 字节偏移量
     */
    @TableField("byte_offset")
    private Integer byteOffset;

    /**
     * 位偏移量
     */
    @TableField("bit_offset")
    private Integer bitOffset;

    /**
     * 位长度
     */
    @TableField("bit_length")
    private Integer bitLength;

    /**
     * 值类型
     */
    @TableField("value_type")
    private String valueType;

    /**
     * 字节序
     */
    @TableField("endianness")
    private String endianness;
}
