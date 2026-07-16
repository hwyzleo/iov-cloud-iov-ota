package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 按阶段审批要求持久化对象
 * 对应表：tb_phase_approval_policy
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_phase_approval_policy")
public class PhaseApprovalPolicyPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 阶段：1=VALIDATION, 2=CANARY, 3=RELEASE
     */
    @TableField("phase")
    private Integer phase;

    /**
     * 活动级覆盖（可空，空为全局策略）
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 是否需要审批
     */
    @TableField("required")
    private Boolean required;

    /**
     * 需要审批级别（逗号分隔）：QUALITY,PRODUCT,SECURITY
     */
    @TableField("required_levels")
    private String requiredLevels;
}