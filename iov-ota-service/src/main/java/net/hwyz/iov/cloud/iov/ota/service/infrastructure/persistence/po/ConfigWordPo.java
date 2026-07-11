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
 * 配置字表 数据对象
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
@TableName("tb_config_word")
public class ConfigWordPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置字代码
     */
    @TableField("code")
    private String code;

    /**
     * 配置字名称
     */
    @TableField("name")
    private String name;

    /**
     * 设备代码
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 数据格式
     */
    @TableField("data_format")
    private String dataFormat;

    /**
     * 数据长度
     */
    @TableField("byte_length")
    private Integer byteLength;

    /**
     * 读写能力
     */
    @TableField("rw_capability")
    private String rwCapability;
}
