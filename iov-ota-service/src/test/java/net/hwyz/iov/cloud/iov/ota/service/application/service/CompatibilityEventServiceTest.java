package net.hwyz.iov.cloud.iov.ota.service.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CompatibilityEventService 测试（CR-012 §9.3、US-073）
 *
 * @author hwyz_leo
 */
@DisplayName("CompatibilityEventService v1 兼容事件")
class CompatibilityEventServiceTest {

    private CompatibilityEventService service;

    @BeforeEach
    void setUp() {
        service = new CompatibilityEventService();
    }

    @Test
    @DisplayName("生成服务端 eventId 和递增 sequenceNo")
    void composeEvent_generatesServerSideIdAndSequence() {
        CompatibilityEventService.CompatibleEvent e1 = service.composeEvent(100L, "VIN001", "PROCESS", "1");
        CompatibilityEventService.CompatibleEvent e2 = service.composeEvent(100L, "VIN001", "PROCESS", "2");

        assertNotNull(e1.eventId());
        assertTrue(e1.eventId().startsWith("v1-"));
        assertEquals(1L, e1.sequenceNo());
        assertEquals(2L, e2.sequenceNo());
        assertNotNull(e1.occurredAt());
    }

    @Test
    @DisplayName("不同任务独立计数")
    void composeEvent_differentTasks_independentSequence() {
        CompatibilityEventService.CompatibleEvent t1 = service.composeEvent(100L, "VIN001", "PROCESS", "1");
        CompatibilityEventService.CompatibleEvent t2 = service.composeEvent(200L, "VIN002", "PROCESS", "1");

        assertEquals(1L, t1.sequenceNo());
        assertEquals(1L, t2.sequenceNo());
    }

    @Test
    @DisplayName("eventId 全局唯一")
    void composeEvent_eventIdUnique() {
        CompatibilityEventService.CompatibleEvent e1 = service.composeEvent(100L, "VIN001", "STATE", "1");
        CompatibilityEventService.CompatibleEvent e2 = service.composeEvent(100L, "VIN001", "STATE", "2");

        assertNotEquals(e1.eventId(), e2.eventId());
    }

    @Test
    @DisplayName("携带事件类型和负载")
    void composeEvent_carriesTypeAndPayload() {
        CompatibilityEventService.CompatibleEvent e = service.composeEvent(100L, "VIN001", "STATE", "99");

        assertEquals("STATE", e.eventType());
        assertEquals("99", e.payload());
        assertEquals("VIN001", e.vin());
        assertEquals(100L, e.taskId());
    }
}
