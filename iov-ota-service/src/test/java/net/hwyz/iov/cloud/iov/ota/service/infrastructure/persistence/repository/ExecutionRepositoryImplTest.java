package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.ExecutionConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionActiveMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.OtaExecutionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionActivePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.OtaExecutionPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExecutionRepositoryImpl 水位持久化回归测试（2026-11-15 实车问题）。
 *
 * <p>缺陷：`ExecutionConverter.toPo` 不写 `rowVersion`，MyBatis-Plus 乐观锁因 version 为 null
 * 直接跳过，`updateById` 变成整行覆盖写；任何基于旧快照的写都会把 `accepted_sequence_no`
 * 写回小值（车端已 ACKED seq2，云端水位回退到 1，seq3.. 永久 BUFFERED）。修复后必须：
 * 水位单调不减、更新 0 行时不静默吞掉。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExecutionRepositoryImpl 事件水位持久化")
class ExecutionRepositoryImplTest {

    @Mock private OtaExecutionMapper otaExecutionMapper;
    @Mock private ExecutionActiveMapper executionActiveMapper;
    @Mock private ExecutionConverter converter;

    @InjectMocks
    private ExecutionRepositoryImpl repository;

    private Execution execution;

    @BeforeEach
    void setUp() {
        Instant validUntil = Instant.now().plusSeconds(3600);
        execution = Execution.permit(ExecutionId.of(1001L), VehicleTaskId.of(4L), 1,
                TaskRevision.initial(), "task-1-r1", SnapshotDigest.of("digest"), "cond-v1",
                PermitToken.of("token", validUntil), validUntil);
        // 事件 1、2 连续到达 → 水位 2
        execution.receiveEvent(1L);
        execution.receiveEvent(2L);

        OtaExecutionPo po = new OtaExecutionPo();
        po.setId(1001L);
        po.setVehicleTaskId(4L);
        po.setAttemptNo(1);
        when(converter.toPo(any(Execution.class))).thenReturn(po);
        when(otaExecutionMapper.selectById(1001L)).thenReturn(po);
        when(otaExecutionMapper.updateById(any())).thenReturn(1);
        when(otaExecutionMapper.updateAcceptedSequenceNoMonotonic(anyLong(), anyLong())).thenReturn(1);
        when(executionActiveMapper.selectByVehicleTaskId(4L)).thenReturn(
                ExecutionActivePo.builder().vehicleTaskId(4L).executionId(1001L).build());
    }

    @Test
    @DisplayName("保存时以 GREATEST 单调推进水位，避免整行覆盖写把水位写回小值")
    void save_updatesWatermarkMonotonically() {
        repository.save(execution);

        verify(otaExecutionMapper).updateById(any());
        verify(otaExecutionMapper).updateAcceptedSequenceNoMonotonic(1001L, 2L);
    }

    @Test
    @DisplayName("更新影响 0 行时抛出可恢复异常，不静默吞掉事件")
    void save_zeroRowsAffected_throws() {
        when(otaExecutionMapper.updateById(any())).thenReturn(0);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> repository.save(execution));

        assertTrue(thrown.getMessage().contains("更新失败"));
        verify(otaExecutionMapper, never()).updateAcceptedSequenceNoMonotonic(anyLong(), anyLong());
    }

    @Test
    @DisplayName("行不存在时走插入分支，不做单调水位更新")
    void save_notExisting_insertsWithoutMonotonicUpdate() {
        when(otaExecutionMapper.selectById(1001L)).thenReturn(null);

        repository.save(execution);

        verify(otaExecutionMapper).insert(any());
        verify(otaExecutionMapper, never()).updateAcceptedSequenceNoMonotonic(anyLong(), anyLong());
    }
}
