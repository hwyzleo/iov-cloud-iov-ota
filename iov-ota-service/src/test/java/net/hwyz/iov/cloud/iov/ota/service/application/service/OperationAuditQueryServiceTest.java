package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskStateLogResult;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskStateLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStateLogPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CR-015 P2-A 任务状态迁移审计查询测试（§3.4）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperationAuditQueryService 状态迁移审计")
class OperationAuditQueryServiceTest {

    @Mock private TaskStateLogMapper taskStateLogMapper;

    @InjectMocks
    private OperationAuditQueryService service;

    private TaskStateLogPo log(int from, int to, String action, String operator, LocalDateTime decidedAt) {
        return TaskStateLogPo.builder()
                .taskId(1L)
                .fromState(from)
                .toState(to)
                .action(action)
                .operator(operator)
                .reason("测试")
                .decidedAt(decidedAt)
                .build();
    }

    @Test
    @DisplayName("查询状态迁移审计并映射状态名（mock 列表按 DB 排序返回）")
    void listStateLogs_mapsAndOrders() {
        // DB 已按 decided_at DESC, id DESC 排序返回
        when(taskStateLogMapper.selectList(any())).thenReturn(List.of(
                log(3, 5, "SCHEDULE", "ops", LocalDateTime.of(2026, 8, 2, 10, 0)),
                log(1, 2, "SUBMIT", "ops", LocalDateTime.of(2026, 8, 1, 10, 0))));

        List<TaskStateLogResult> results = service.listStateLogs(1L, null, null, null);

        assertEquals(2, results.size());
        // 最新在前
        assertEquals("SCHEDULE", results.get(0).getAction());
        assertEquals("APPROVED", results.get(0).getFromState());
        assertEquals("SCHEDULED", results.get(0).getToState());
        assertEquals("SUBMIT", results.get(1).getAction());
        assertEquals("DRAFT", results.get(1).getFromState());
        assertEquals("PENDING_APPROVAL", results.get(1).getToState());
    }

    @Test
    @DisplayName("按操作类型与时间范围筛选")
    void listStateLogs_filters() {
        when(taskStateLogMapper.selectList(any())).thenReturn(List.of(
                log(1, 2, "SUBMIT", "ops", LocalDateTime.of(2026, 8, 1, 10, 0))));

        Date begin = Date.from(LocalDateTime.of(2026, 8, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(LocalDateTime.of(2026, 8, 2, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

        List<TaskStateLogResult> results = service.listStateLogs(1L, begin, end, "SUBMIT");

        assertEquals(1, results.size());
        assertEquals("SUBMIT", results.get(0).getAction());
    }

    @Test
    @DisplayName("未知状态值按原始数字展示")
    void listStateLogs_unknownState_rawValue() {
        when(taskStateLogMapper.selectList(any())).thenReturn(List.of(
                log(99, 1, "SUBMIT", "ops", LocalDateTime.of(2026, 8, 1, 10, 0))));

        List<TaskStateLogResult> results = service.listStateLogs(1L, null, null, null);

        assertEquals("99", results.get(0).getFromState());
        assertEquals("DRAFT", results.get(0).getToState());
    }
}
