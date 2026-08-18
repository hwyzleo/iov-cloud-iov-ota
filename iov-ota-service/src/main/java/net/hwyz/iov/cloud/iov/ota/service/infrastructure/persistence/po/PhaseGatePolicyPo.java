package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 阶段门禁阈值策略持久化对象
 * 对应表：tb_phase_gate_policy
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_phase_gate_policy")
public class PhaseGatePolicyPo extends BasePo {

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
     * 成功率最小阈值
     */
    @TableField("success_rate_min")
    private BigDecimal successRateMin;

    /**
     * 失败数最大阈值
     */
    @TableField("fail_cnt_max")
    private Integer failCntMax;

    /**
     * 是否允许严重缺陷
     */
    @TableField("severe_defect_allowed")
    private Boolean severeDefectAllowed;
}
