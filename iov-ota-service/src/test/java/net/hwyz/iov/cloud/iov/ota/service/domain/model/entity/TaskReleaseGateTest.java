package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CR-015 放行门禁领域实体测试
 *
 * @author hwyz_leo
 */
@DisplayName("TaskReleaseGate 领域实体")
class TaskReleaseGateTest {

    private TaskReleaseGate gate() {
        return TaskReleaseGate.builder()
                .activityId(100L)
                .previousTaskId(1L)
                .nextTaskId(2L)
                .gateType(TaskReleaseGate.ReleaseGateType.SAME_PHASE)
                .gateState(ReleaseGateState.PENDING)
                .override(false)
                .build();
    }

    @Test
    @DisplayName("初始 PENDING 时 isPending=true、isPassed/isFailed=false")
    void pending_initialState() {
        TaskReleaseGate gate = gate();
        assertTrue(gate.isPending());
        assertFalse(gate.isPassed());
        assertFalse(gate.isFailed());
    }

    @Test
    @DisplayName("pass() 置为 PASS 并记录决策人/报告引用")
    void pass_marksPassed() {
        TaskReleaseGate gate = gate();
        gate.pass("ops", "1:2");
        assertTrue(gate.isPassed());
        assertEquals(ReleaseGateState.PASS, gate.getGateState());
        assertEquals("ops", gate.getDecidedBy());
        assertEquals("1:2", gate.getReportRef());
        assertNotNull(gate.getDecidedAt());
    }

    @Test
    @DisplayName("fail() 置为 FAIL 并记录决策人/报告引用")
    void fail_marksFailed() {
        TaskReleaseGate gate = gate();
        gate.fail("ops", "1:1");
        assertTrue(gate.isFailed());
        assertEquals(ReleaseGateState.FAIL, gate.getGateState());
        assertEquals("ops", gate.getDecidedBy());
    }

    @Test
    @DisplayName("override() 人工放行：置 override=true、PASS，并固化原因")
    void override_marksOverride() {
        TaskReleaseGate gate = gate();
        gate.override("ops", "APR-001", "人工放行验证");
        assertTrue(Boolean.TRUE.equals(gate.getOverride()));
        assertTrue(gate.isPassed());
        assertEquals(ReleaseGateState.PASS, gate.getGateState());
        assertEquals("APR-001", gate.getApprovalRef());
        assertEquals("人工放行验证", gate.getDescription());
        assertEquals("ops", gate.getDecidedBy());
    }
}
