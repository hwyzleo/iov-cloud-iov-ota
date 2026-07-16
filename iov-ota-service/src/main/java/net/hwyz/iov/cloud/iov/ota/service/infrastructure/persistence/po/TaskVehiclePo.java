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
     * 车辆任务状态
     */
    @TableField("state")
    private Integer state;

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
}
