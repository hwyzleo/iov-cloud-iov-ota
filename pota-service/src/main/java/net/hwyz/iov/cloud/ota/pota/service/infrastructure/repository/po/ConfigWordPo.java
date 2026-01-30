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
 * 配置字表 数据对象
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
@TableName("tb_config_word")
public class ConfigWordPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型：1-固定配置字，2-软件内部版本
     */
    @TableField("type")
    private Integer type;

    /**
     * 关联ID
     */
    @TableField("reference_id")
    private Long referenceId;

    /**
     * 设备代码
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 软件零件号
     */
    @TableField("software_pn")
    private String softwarePn;

    /**
     * 配置字版本
     */
    @TableField("config_word_version")
    private String configWordVersion;

    /**
     * 起始byte
     */
    @TableField("start_byte")
    private Short startByte;

    /**
     * 起始bit
     */
    @TableField("start_bit")
    private Short startBit;

    /**
     * 配置字值
     */
    @TableField("config_word_value")
    private String configWordValue;

    /**
     * 依赖设备
     */
    @TableField("depend_device")
    private String dependDevice;

    /**
     * 依赖设备软件零件号
     */
    @TableField("depend_device_software_pn")
    private String dependDeviceSoftwarePn;
}
