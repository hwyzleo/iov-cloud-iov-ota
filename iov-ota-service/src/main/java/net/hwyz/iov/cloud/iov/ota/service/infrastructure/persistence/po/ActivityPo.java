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
     * 活动编码（系统生成·全局唯一·不可变）
     */
    @TableField("activity_code")
    private String activityCode;

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
     * 升级目的：1 缺陷修复 2 功能新增 3 安全补丁 4 合规整改 9 其他
     */
    @TableField("upgrade_purpose")
    private Integer upgradePurpose;

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
     * 总文件大小（字节）
     */
    @TableField("total_file_size")
    private Long totalFileSize;

    /**
     * 文件大小缓存计算时间
     */
    @TableField("size_calc_time")
    private Date sizeCalcTime;

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

    /**
     * 是否型批相关
     */
    @TableField("is_type_approval_relevant")
    private Boolean isTypeApprovalRelevant;

    /**
     * 型批影响评估状态：0 未评估 1 通过 2 阻断
     */
    @TableField("type_approval_assessment_state")
    private Integer typeApprovalAssessmentState;

    /**
     * 发行说明文章ID
     */
    @TableField("release_note_article_id")
    private Long releaseNoteArticleId;

    /**
     * 须知是否需显式同意
     */
    @TableField("notice_consent_required")
    private Boolean noticeConsentRequired;

    /**
     * 条款是否需显式同意
     */
    @TableField("terms_consent_required")
    private Boolean termsConsentRequired;

    /**
     * 隐私是否需显式同意
     */
    @TableField("privacy_consent_required")
    private Boolean privacyConsentRequired;

    /**
     * RXSWIN值（只读·manifest回填·仅1:1场景）
     */
    @TableField("rxswin")
    private String rxswin;
}
