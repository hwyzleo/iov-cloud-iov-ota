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
 * tb_type_approval_baseline 数据对象
 * 型式批准基线投影表（MDM EEAD TypeApprovalBaseline）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_type_approval_baseline")
public class TypeApprovalBaselinePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * TA基线代码（业务键）
     */
    @TableField("ta_baseline_code")
    private String taBaselineCode;

    /**
     * SWIN代码
     */
    @TableField("swin_code")
    private String swinCode;

    /**
     * 锚定类型：VARIANT / MODEL
     */
    @TableField("anchor_type")
    private String anchorType;

    /**
     * 锚定代码
     */
    @TableField("anchor_code")
    private String anchorCode;

    /**
     * 状态：仅消费 RELEASED / FROZEN
     */
    @TableField("status")
    private String status;

    /**
     * 型批版本组合摘要（sha256(sortedItems)）
     */
    @TableField("projection_digest")
    private String projectionDigest;

    /**
     * 生效时间
     */
    @TableField("effective_from")
    private Date effectiveFrom;

    /**
     * 溯源：来源基线范围
     */
    @TableField("source_baseline_scope")
    private String sourceBaselineScope;

    /**
     * 上游版本号（幂等键之一）
     */
    @TableField("up_version")
    private Long upVersion;

    /**
     * 最后同步时间
     */
    @TableField("sync_time")
    private Date syncTime;

}
