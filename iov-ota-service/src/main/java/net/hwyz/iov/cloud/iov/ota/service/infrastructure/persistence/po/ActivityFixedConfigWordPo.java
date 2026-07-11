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
 * 升级活动固定配置字表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-23
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_activity_fixed_config_word")
public class ActivityFixedConfigWordPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 升级活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 固定配置字ID
     */
    @TableField("fixed_config_word_id")
    private Long fixedConfigWordId;

    /**
     * 配置字代码
     */
    @TableField(exist = false)
    private String configWordCode;

    /**
     * 配置字名称
     */
    @TableField(exist = false)
    private String configWordName;

    /**
     * 设备代码
     */
    @TableField(exist = false)
    private String deviceCode;
}
