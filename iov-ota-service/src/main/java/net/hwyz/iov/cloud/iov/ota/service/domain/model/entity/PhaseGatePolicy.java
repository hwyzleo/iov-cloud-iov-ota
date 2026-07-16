package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;

import java.math.BigDecimal;

/**
 * 阶段门禁阈值策略领域实体
 * 对应表：tb_phase_gate_policy
 */
@Getter
@Setter
@Builder
public class PhaseGatePolicy {
    
    private Long id;
    private TaskPhase phase;
    private Long activityId;  // 活动级覆盖（可空，空为全局策略）
    private BigDecimal successRateMin;  // 成功率最小阈值
    private Integer failCntMax;  // 失败数最大阈值
    private Boolean severeDefectAllowed;  // 是否允许严重缺陷
    
    /**
     * 检查指标是否满足门禁阈值
     * @param successRate 成功率
     * @param failCnt 失败数
     * @param severeDefectCount 严重缺陷数
     * @return 是否满足
     */
    public boolean checkThreshold(BigDecimal successRate, Integer failCnt, Integer severeDefectCount) {
        // 检查成功率
        if (successRateMin != null && successRate != null && successRate.compareTo(successRateMin) < 0) {
            return false;
        }
        
        // 检查失败数
        if (failCntMax != null && failCnt != null && failCnt > failCntMax) {
            return false;
        }
        
        // 检查严重缺陷
        if (!severeDefectAllowed && severeDefectCount != null && severeDefectCount > 0) {
            return false;
        }
        
        return true;
    }
}