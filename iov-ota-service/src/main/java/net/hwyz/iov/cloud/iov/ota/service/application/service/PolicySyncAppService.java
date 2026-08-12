package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.PolicySyncCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.PolicySyncResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import org.springframework.stereotype.Service;

/**
 * 策略同步应用服务（CR-012 §5.9、US-084）
 *
 * <p>用户偏好独立版本化，使用 basePreferenceVersion 乐观并发。
 * 有效策略由活动策略、法规要求和用户偏好合并。
 * 已冻结任务的实质变化必须升级 taskRevision；执行中的控制变化必须升级 controlRevision。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySyncAppService {

    private final VehicleTaskRepository vehicleTaskRepository;

    /**
     * 同步用户偏好与有效策略。
     *
     * @param cmd 策略同步命令
     * @return 策略同步结果
     */
    public PolicySyncResult sync(PolicySyncCmd cmd) {
        log.info("车辆[{}]策略同步，基础版本[{}]", cmd.getVin(), cmd.getBasePreferenceVersion());

        // TODO: 合并活动策略、法规要求和用户偏好为有效策略
        // 当前为占位实现，返回用户偏好作为有效策略
        String effectivePolicy = cmd.getUserPreference() != null
                ? cmd.getUserPreference()
                : "{}";

        boolean revisionUpgradeRequired = false;

        // 若关联车辆任务，检查实质性变化是否需升级 taskRevision
        if (cmd.getVehicleTaskId() != null) {
            VehicleTask vt = vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId())).orElse(null);
            if (vt != null && !vt.isTerminal()) {
                // TODO: 判定用户偏好变化是否为实质性变化
                // 非实质性 revision 变化不自动使 receipt 失效
                revisionUpgradeRequired = false;
            }
        }

        long preferenceVersion = cmd.getBasePreferenceVersion() != null
                ? cmd.getBasePreferenceVersion() + 1
                : 1L;

        return PolicySyncResult.builder()
                .effectivePolicy(effectivePolicy)
                .preferenceVersion(preferenceVersion)
                .revisionUpgradeRequired(revisionUpgradeRequired)
                .build();
    }
}
