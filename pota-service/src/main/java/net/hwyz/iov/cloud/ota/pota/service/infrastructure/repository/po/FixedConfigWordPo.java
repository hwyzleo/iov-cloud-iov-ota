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
 * 固定配置字表 数据对象
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
@TableName("tb_fixed_config_word")
public class FixedConfigWordPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
     * 分类
     */
    @TableField("type")
    private Short type;
}
