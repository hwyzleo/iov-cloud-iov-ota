package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CR-015 放行门禁计算领域服务测试
 *
 * @author hwyz_leo
 */
@DisplayName("TaskReleaseGateDomainService 门禁计算")
class TaskReleaseGateDomainServiceTest {

    private final TaskReleaseGateDomainService service = new TaskReleaseGateDomainService();

    @Test
    @DisplayName("无策略时默认 PASS（保持门禁不存在默认通过语义）")
    void noPolicy_defaultPass() {
        assertEquals(ReleaseGateState.PASS, service.evaluate(null, BigDecimal.valueOf(0.9), 1));
    }

    @Test
    @DisplayName("策略满足阈值 -> PASS")
    void policyMet_pass() {
        PhaseGatePolicy policy = PhaseGatePolicy.builder()
                .phase(TaskPhase.CANARY)
                .successRateMin(BigDecimal.valueOf(0.95))
                .failCntMax(5)
                .severeDefectAllowed(false)
                .build();
        assertEquals(ReleaseGateState.PASS,
                service.evaluate(policy, BigDecimal.valueOf(0.98), 2));
    }

    @Test
    @DisplayName("成功率低于阈值 -> FAIL")
    void lowSuccessRate_fail() {
        PhaseGatePolicy policy = PhaseGatePolicy.builder()
                .successRateMin(BigDecimal.valueOf(0.95))
                .failCntMax(5)
                .build();
        assertEquals(ReleaseGateState.FAIL,
                service.evaluate(policy, BigDecimal.valueOf(0.90), 2));
    }

    @Test
    @DisplayName("失败数超过阈值 -> FAIL")
    void highFailCnt_fail() {
        PhaseGatePolicy policy = PhaseGatePolicy.builder()
                .successRateMin(BigDecimal.valueOf(0.80))
                .failCntMax(3)
                .build();
        assertEquals(ReleaseGateState.FAIL,
                service.evaluate(policy, BigDecimal.valueOf(0.85), 6));
    }

    @Test
    @DisplayName("有策略但报告统计缺失 -> PENDING（fail-safe）")
    void missingReportStats_pending() {
        PhaseGatePolicy policy = PhaseGatePolicy.builder()
                .successRateMin(BigDecimal.valueOf(0.95))
                .failCntMax(3)
                .build();
        assertEquals(ReleaseGateState.PENDING, service.evaluate(policy, null, null));
    }

    @Test
    @DisplayName("阈值快照序列化为 JSON")
    void thresholdSnapshot_json() {
        PhaseGatePolicy policy = PhaseGatePolicy.builder()
                .phase(TaskPhase.RELEASE)
                .activityId(100L)
                .successRateMin(BigDecimal.valueOf(0.90))
                .failCntMax(10)
                .build();
        String snapshot = service.toThresholdSnapshot(policy);
        assertTrue(snapshot.contains("successRateMin"));
        assertTrue(snapshot.contains("failCntMax"));
        assertTrue(snapshot.contains("0.9"));
    }
}
