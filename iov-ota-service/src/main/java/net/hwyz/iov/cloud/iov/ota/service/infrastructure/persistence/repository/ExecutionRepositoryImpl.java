package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.ExecutionConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionActiveMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.OtaExecutionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionActivePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.OtaExecutionPo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 安装执行仓库实现（CR-012 §2.3、§8）
 *
 * <p>活动 Execution 唯一性通过 tb_task_vehicle_execution_active 占位表保证（RD-012-5）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ExecutionRepositoryImpl implements ExecutionRepository {

    private final OtaExecutionMapper otaExecutionMapper;
    private final ExecutionActiveMapper executionActiveMapper;
    private final ExecutionConverter converter;

    @Override
    public Optional<Execution> getById(ExecutionId id) {
        OtaExecutionPo po = otaExecutionMapper.selectById(id.getValue());
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public Optional<Execution> getByIdForUpdate(ExecutionId id) {
        OtaExecutionPo po = otaExecutionMapper.selectByIdForUpdate(id.getValue());
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public Optional<Execution> getByVehicleTaskIdAndAttemptNo(VehicleTaskId vehicleTaskId, int attemptNo) {
        OtaExecutionPo po = otaExecutionMapper.selectByVehicleTaskIdAndAttemptNo(
                vehicleTaskId.getValue(), attemptNo);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public Optional<Execution> findActiveByVehicleTaskId(VehicleTaskId vehicleTaskId) {
        ExecutionActivePo activePo = executionActiveMapper.selectByVehicleTaskId(vehicleTaskId.getValue());
        if (activePo == null) {
            return Optional.empty();
        }
        OtaExecutionPo po = otaExecutionMapper.selectById(activePo.getExecutionId());
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    @Transactional
    public void save(Execution execution) {
        OtaExecutionPo po = converter.toPo(execution);
        if (po.getId() != null && otaExecutionMapper.selectById(po.getId()) != null) {
            int updated = otaExecutionMapper.updateById(po);
            if (updated == 0 && otaExecutionMapper.selectById(po.getId()) == null) {
                // 行被删/不可见时不得静默吞掉：向上抛可恢复异常，由消费侧重投而不是丢事件
                throw new IllegalStateException("执行[" + po.getId() + "]更新失败：目标行不存在");
            }
            // updated == 0 且行仍存在 = 整行内容与库中完全一致（MySQL affected-rows 语义），
            // 属幂等重放的正常 no-op，不得当作异常（实车 2026-11-15：幂等重放被误判为
            // 「行不存在」→ 可恢复重投 3 次耗尽 → 隔离/DLQ → 全链路无下行）。
            if (updated == 0) {
                log.debug("执行[{}]状态未变化，跳过无效整行更新", po.getId());
            }
            // 水位单调保护：整行写回可能携带旧快照的 acceptedSequenceNo，
            // 这里用 GREATEST 强制只增不减（实车 2026-11-15：水位从 2 回退到 1）。
            otaExecutionMapper.updateAcceptedSequenceNoMonotonic(
                    po.getId(), execution.getSequenceWatermark().getAcceptedSequenceNo());
        } else {
            otaExecutionMapper.insert(po);
        }

        // 维护活动执行占位表
        if (execution.isActive()) {
            ExecutionActivePo activePo = executionActiveMapper.selectByVehicleTaskId(
                    execution.getVehicleTaskId().getValue());
            if (activePo == null) {
                ExecutionActivePo newActive = ExecutionActivePo.builder()
                        .vehicleTaskId(execution.getVehicleTaskId().getValue())
                        .executionId(execution.getId().getValue())
                        .build();
                executionActiveMapper.insert(newActive);
            } else if (!activePo.getExecutionId().equals(execution.getId().getValue())) {
                throw new IllegalStateException(
                        "车辆任务[" + execution.getVehicleTaskId().getValue()
                                + "]已存在活动执行，占位表冲突");
            }
        } else {
            executionActiveMapper.deleteByVehicleTaskId(execution.getVehicleTaskId().getValue());
        }
    }
}
