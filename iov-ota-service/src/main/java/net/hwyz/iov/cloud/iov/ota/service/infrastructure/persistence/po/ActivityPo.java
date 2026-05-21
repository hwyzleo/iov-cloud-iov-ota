package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 升级活动表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-09-12
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_activity")
public class ActivityPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动名称
     */
    @TableField("name")
    private String name;

    /**
     * 活动版本
     */
    @TableField("version")
    private String version;

    /**
     * 升级须知文章ID
     */
    @TableField("upgrade_notice_article_id")
    private Long upgradeNoticeArticleId;

    /**
     * 活动条款文章ID
     */
    @TableField("activity_term_article_id")
    private Long activityTermArticleId;

    /**
     * 隐私协议文章ID
     */
    @TableField("privacy_agreement_article_id")
    private Long privacyAgreementArticleId;

    /**
     * 活动开始时间
     */
    @TableField("start_time")
    private Date startTime;

    /**
     * 活动结束时间
     */
    @TableField("end_time")
    private Date endTime;

    /**
     * 活动发布时间
     */
    @TableField("release_time")
    private Date releaseTime;

    /**
     * 升级目的
     */
    @TableField("upgrade_purpose")
    private String upgradePurpose;

    /**
     * 升级功能项
     */
    @TableField("upgrade_function")
    private String upgradeFunction;

    /**
     * 活动说明
     */
    @TableField("statement")
    private String statement;

    /**
     * 活动状态：1 待提交，2 待审核，3 已审核，4 未通过，5 已发布，6 已结束，7 已取消
     */
    @TableField("state")
    private Integer state;

    /**
     * 总文件大小（MB）
     */
    @TableField("total_file_size")
    private Long totalFileSize;

    /**
     * 是否基线活动
     */
    @TableField("baseline")
    private Boolean baseline;

    /**
     * 基线代码
     */
    @TableField("baseline_code")
    private String baselineCode;
}
