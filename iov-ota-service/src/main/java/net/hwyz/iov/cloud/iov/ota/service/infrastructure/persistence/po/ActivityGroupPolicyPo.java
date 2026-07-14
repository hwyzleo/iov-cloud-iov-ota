package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 活动分组策略表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-13
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_activity_group_policy")
public class ActivityGroupPolicyPo extends BasePo {

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
     * 活动内分组号
     */
    @TableField("group_no")
    private Integer groupNo;

    /**
     * 是否同升同降：1-是，0-否
     */
    @TableField("rollback_together")
    private Boolean rollbackTogether;

    /**
     * 是否原子激活：1-是，0-否
     */
    @TableField("atomic_activation")
    private Boolean atomicActivation;

    /**
     * 是否统一重启：1-是，0-否
     */
    @TableField("unified_reboot")
    private Boolean unifiedReboot;

    /**
     * 失败策略：0 全组回滚，1 保持旧版，2 重试后回滚
     */
    @TableField("failure_policy")
    private Integer failurePolicy;

    /**
     * 失败阈值（失败数量达到阈值触发策略）
     */
    @TableField("fail_threshold")
    private Integer failThreshold;
}