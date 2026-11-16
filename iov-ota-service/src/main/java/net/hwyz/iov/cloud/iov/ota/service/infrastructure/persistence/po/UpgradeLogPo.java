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
 * tb_upgrade_log 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_upgrade_log")
public class UpgradeLogPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
     * 车辆任务ID（过程关联主锚点，CR-026）
     */
    @TableField("vehicle_task_id")
    private Long vehicleTaskId;

    /**
     * 安装执行ID（可空，CR-026）
     */
    @TableField("execution_id")
    private Long executionId;

    /**
     * 日志上传申请ID（EDD-OSS requestId，CR-026）
     */
    @TableField("log_request_id")
    private String logRequestId;

    /**
     * 采集范围（CR-026）
     */
    @TableField("log_scope")
    private String logScope;

    /**
     * 脱敏版本（CR-026）
     */
    @TableField("desensitize_version")
    private String desensitizeVersion;

    /**
     * 隐私分级（CR-026）
     */
    @TableField("privacy_level")
    private String privacyLevel;

    /**
     * 对象存储键（事实锚点，CR-026）
     */
    @TableField("object_key")
    private String objectKey;

    /**
     * EDD-OSS 日志对象ID（complete/查询必需，CR-026）
     */
    @TableField("log_object_id")
    private String logObjectId;

    /**
     * 对象字节数（CR-026）
     */
    @TableField("object_size_bytes")
    private Long objectSizeBytes;

    /**
     * 日志摘要（CR-026）
     */
    @TableField("log_digest")
    private String logDigest;

    /**
     * 摘要算法（CR-026）
     */
    @TableField("digest_algorithm")
    private String digestAlgorithm;

    /**
     * 稳定失败原因码（CR-026）
     */
    @TableField("failure_reason")
    private String failureReason;

    /**
     * 上传结果（CR-026）
     */
    @TableField("upload_result")
    private String uploadResult;

    /**
     * 授权登记时间（CR-026）
     */
    @TableField("authorized_at")
    private Date authorizedAt;

    /**
     * 对象核验通过时间（CR-026）
     */
    @TableField("uploaded_at")
    private Date uploadedAt;

    /**
     * 结果受理时间（CR-026）
     */
    @TableField("received_at")
    private Date receivedAt;

    /**
     * 日志URL（历史兼容，不再作为永久权威凭证）
     */
    @TableField("log_url")
    private String logUrl;

    /**
     * 上传状态
     */
    @TableField("upload_state")
    private String uploadState;

    /**
     * 上传时间
     */
    @TableField("upload_time")
    private Date uploadTime;

}
