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
 * 兼容零件号表 数据对象
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
@TableName("tb_compatible_pn")
public class CompatiblePnPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型：1-软件零件号，2-硬件零件号
     */
    @TableField("type")
    private Short type;

    /**
     * 设备代码
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 零件号
     */
    @TableField("pn")
    private String pn;

    /**
     * 兼容零件号
     */
    @TableField("compatible_pn")
    private String compatiblePn;
}
