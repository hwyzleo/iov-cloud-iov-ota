package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 升级任务车辆表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle")
public class TaskVehiclePo extends BasePo {

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
     * 升级任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 结果代码
     */
    @TableField("result_code")
    private String resultCode;

    /**
     * 目标来源：CONDITION/LIST/IMPORT
     */
    @TableField("source")
    private String source;

    /**
     * 准入状态：PASS/REJECT
     */
    @TableField("admit_state")
    private String admitState;

    /**
     * 准入原因（REJECT时）
     */
    @TableField("admit_reason")
    private String admitReason;

    /**
     * 基线快照
     */
    @TableField("baseline")
    private String baseline;

    /**
     * 下载重试次数
     */
    @TableField("download_retry_count")
    private Integer downloadRetryCount;

    /**
     * 安装重试次数
     */
    @TableField("install_retry_count")
    private Integer installRetryCount;

    /**
     * 续传偏移量（字节）
     */
    @TableField("resume_offset")
    private Long resumeOffset;

    /**
     * 续传令牌
     */
    @TableField("resume_token")
    private String resumeToken;

    /**
     * 最近失败原因
     */
    @TableField("last_fail_reason")
    private String lastFailReason;

    /**
     * 下次重试时间
     */
    @TableField("next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * 尝试次数（幂等）
     */
    @TableField("attempt_no")
    private Integer attemptNo;

    // ==================== CR-012 VehicleTask 字段 ====================

    /**
     * 车辆任务状态（CR-012）
     */
    @TableField("vehicle_task_status")
    private String vehicleTaskStatus;

    /**
     * 任务版本号（CR-012）
     */
    @TableField("task_revision")
    private Long taskRevision;

    /**
     * 快照摘要（CR-012）
     */
    @TableField("snapshot_digest")
    private String snapshotDigest;

    /**
     * 可用性状态（CR-012）
     */
    @TableField("availability_status")
    private String availabilityStatus;

    /**
     * 下载准备状态（CR-012）
     */
    @TableField("download_ready_state")
    private String downloadReadyState;

    /**
     * 授权状态（CR-012/CR-016）
     */
    @TableField("consent_state")
    private String consentState;

    /**
     * 是否需授权，发布时冻结（CR-016）
     */
    @TableField("consent_required")
    private Integer consentRequired;

    /**
     * 发布冻结条款身份引用（CR-016）
     */
    @TableField("consent_article_id")
    private Long consentArticleId;

    /**
     * 发布冻结条款展示版本（CR-016）
     */
    @TableField("consent_article_version")
    private String consentArticleVersion;

    /**
     * 发布冻结条款权威摘要（CR-016）
     */
    @TableField("consent_article_hash")
    private String consentArticleHash;

    /**
     * 当前授权范围权威摘要（CR-016）
     */
    @TableField("consent_scope_digest")
    private String consentScopeDigest;

    /**
     * 当前权威授权记录ID（CR-016）
     */
    @TableField("current_consent_id")
    private Long currentConsentId;

    /**
     * 当前授权状态最后推进时间（CR-016）
     */
    @TableField("consent_updated_at")
    private java.util.Date consentUpdatedAt;

    /**
     * 发布时间快照（CR-012）
     */
    @TableField("release_at")
    private java.util.Date releaseAt;

    /**
     * 执行窗口开始时间快照（CR-012）
     */
    @TableField("vt_start_time")
    private java.util.Date vtStartTime;

    /**
     * 执行窗口结束时间快照（CR-012）
     */
    @TableField("vt_end_time")
    private java.util.Date vtEndTime;

    /**
     * 最近尝试序号（CR-012）
     */
    @TableField("last_attempt_no")
    private Integer lastAttemptNo;

    /**
     * 活动执行ID（CR-012）
     */
    @TableField("active_execution_id")
    private Long activeExecutionId;

    /**
     * 取代者车辆任务ID（CR-012）
     */
    @TableField("superseded_by")
    private Long supersededBy;

    /**
     * 本地任务处置意图（CR-012）
     */
    @TableField("local_disposition")
    private String localDisposition;

    /**
     * 包缓存处置意图（CR-012）
     */
    @TableField("package_cache_action")
    private String packageCacheAction;

    /**
     * 暂停前状态（CR-012）
     */
    @TableField("vt_state_before_pause")
    private String vtStateBeforePause;
}
