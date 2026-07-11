package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.Date;

/**
 * tb_baseline 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_baseline")
public class BaselinePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 基线代码
     */
    @TableField("baseline_code")
    private String baselineCode;

    /**
     * 基线名称
     */
    @TableField("name")
    private String name;

    /**
     * 锚定类型
     */
    @TableField("anchor_type")
    private String anchorType;

    /**
     * 锚定代码
     */
    @TableField("anchor_code")
    private String anchorCode;

    /**
     * 基线版本
     */
    @TableField("baseline_version")
    private String baselineVersion;

    /**
     * 基线状态
     */
    @TableField("baseline_status")
    private String baselineStatus;

    /**
     * 数据来源
     */
    @TableField("source")
    private String source;

    /**
     * 最后同步时间
     */
    @TableField("sync_time")
    private Date syncTime;

}
