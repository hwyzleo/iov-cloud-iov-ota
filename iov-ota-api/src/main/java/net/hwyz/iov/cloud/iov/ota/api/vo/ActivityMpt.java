package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台升级活动
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActivityMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动编码（系统生成·全局唯一·不可变）
     */
    private String activityCode;

    /**
     * 升级须知文章ID
     */
    private Long upgradeNoticeArticleId;

    /**
     * 活动条款文章ID
     */
    private Long activityTermArticleId;

    /**
     * 隐私协议文章ID
     */
    private Long privacyAgreementArticleId;

    /**
     * 发行说明文章ID
     */
    private Long releaseNoteArticleId;

    /**
     * 活动开始时间（外层约束窗口起）
     */
    private Date startTime;

    /**
     * 活动结束时间（外层约束窗口止）
     */
    private Date endTime;

    /**
     * 活动发布时间
     */
    private Date releaseTime;

    /**
     * 升级目的：1 缺陷修复 2 功能新增 3 安全补丁 4 合规整改 9 其他
     */
    private Integer upgradePurpose;

    /**
     * 升级功能项
     */
    private String upgradeFunction;

    /**
     * 活动说明
     */
    private String statement;

    /**
     * 活动状态：1 待提交，2 待审核，3 已审核，4 未通过，5 已发布，6 已结束，7 已取消
     */
    private Integer state;

    /**
     * 总文件大小（字节·只读缓存）
     */
    private Long totalFileSize;

    /**
     * 文件大小缓存计算时间
     */
    private Date sizeCalcTime;

    /**
     * 是否基线活动
     */
    private Boolean baseline;

    /**
     * 基线代码
     */
    private String baselineCode;

    /**
     * 是否型批相关
     */
    private Boolean isTypeApprovalRelevant;

    /**
     * 型批影响评估状态：0 未评估 1 通过 2 阻断
     */
    private Integer typeApprovalAssessmentState;

    /**
     * 须知是否需显式同意
     */
    private Boolean noticeConsentRequired;

    /**
     * 条款是否需显式同意
     */
    private Boolean termsConsentRequired;

    /**
     * 隐私是否需显式同意
     */
    private Boolean privacyConsentRequired;

    /**
     * RXSWIN值（只读·manifest回填·仅1:1场景）
     */
    private String rxswin;

    /**
     * 升级对象数量
     */
    private Integer upgradeTargetCount;

    /**
     * 创建时间
     */
    private Date createTime;

}
