package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * tb_regulatory_filing 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_regulatory_filing")
public class RegulatoryFilingPo extends BasePo {

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
     * 备案类型
     */
    @TableField("filing_type")
    private String filingType;

    /**
     * 软件内容引用
     */
    @TableField("sw_content_ref")
    private String swContentRef;

    /**
     * ReleaseNote引用
     */
    @TableField("release_note_ref")
    private String releaseNoteRef;

    /**
     * 备案状态
     */
    @TableField("filing_status")
    private String filingStatus;

    /**
     * 备案编号
     */
    @TableField("filing_no")
    private String filingNo;

}
