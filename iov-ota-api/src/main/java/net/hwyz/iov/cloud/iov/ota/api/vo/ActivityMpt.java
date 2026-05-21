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
     * 活动版本
     */
    private String version;

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
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 活动发布时间
     */
    private Date releaseTime;

    /**
     * 升级目的
     */
    private String upgradePurpose;

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
     * 总文件大小（MB）
     */
    private Long totalFileSize;

    /**
     * 是否基线活动
     */
    private Boolean baseline;

    /**
     * 基线代码
     */
    private String baselineCode;

    /**
     * 软件内部版本数量
     */
    private Integer softwareBuildVersionCount;

    /**
     * 创建时间
     */
    private Date createTime;

}
