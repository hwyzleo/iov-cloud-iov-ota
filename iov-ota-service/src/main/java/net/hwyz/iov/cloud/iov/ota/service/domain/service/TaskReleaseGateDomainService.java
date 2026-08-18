package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 放行门禁计算领域服务（CR-015）
 * <p>基于前序正式报告与门禁阈值策略计算 PASS/FAIL；无策略/无报告为 PENDING（fail-safe 拦截）。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class TaskReleaseGateDomainService {

    /**
     * 计算门禁结论
     * @param policy 门禁阈值策略（可为 null，视为无阈值约束）
     * @param successRate 前序任务成功率（可为 null）
     * @param failCnt 前序任务失败数（可为 null）
     * @return PASS/FAIL/PENDING
     */
    public ReleaseGateState evaluate(PhaseGatePolicy policy, BigDecimal successRate, Integer failCnt) {
        if (policy == null) {
            // 无策略：不拦截（保持旧"门禁不存在默认通过"语义）
            return ReleaseGateState.PASS;
        }
        if (successRate == null || failCnt == null) {
            // 前序报告缺失关键统计：fail-safe 拦截
            return ReleaseGateState.PENDING;
        }
        boolean ok = policy.checkThreshold(successRate, failCnt, 0);
        return ok ? ReleaseGateState.PASS : ReleaseGateState.FAIL;
    }

    /**
     * 生成门禁阈值快照（JSON），随门禁记录冻结
     */
    public String toThresholdSnapshot(PhaseGatePolicy policy) {
        JSONObject json = new JSONObject();
        if (policy == null) {
            json.set("successRateMin", null);
            json.set("failCntMax", null);
            json.set("severeDefectAllowed", null);
            return json.toString();
        }
        json.set("phase", policy.getPhase() != null ? policy.getPhase().name() : null);
        json.set("activityId", policy.getActivityId());
        json.set("successRateMin", policy.getSuccessRateMin());
        json.set("failCntMax", policy.getFailCntMax());
        json.set("severeDefectAllowed", policy.getSevereDefectAllowed());
        return json.toString();
    }
}
