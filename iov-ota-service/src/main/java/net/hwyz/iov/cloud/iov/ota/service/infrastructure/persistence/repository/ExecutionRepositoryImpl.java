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
            otaExecutionMapper.updateById(po);
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
