package net.hwyz.iov.cloud.iov.ota.service.infrastructure.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleProjectionSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 车辆投影周期性对账任务（CR-015 §4.3）
 * <p>按 checkpoint updatedAfter 增量拉取 VMD 快照，记录 scan/add/update/ignore/fail、
 * 最大版本差与同步延迟，作为 Kafka 增量消费的补偿闭环。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.vehicle-projection.reconcile.enabled", havingValue = "true", matchIfMissing = false)
public class VehicleProjectionReconcileJob {

    private final VehicleProjectionSyncService vehicleProjectionSyncService;

    /**
     * 周期性对账（默认每天凌晨 3 点，可通过配置覆盖）
     */
    @Scheduled(cron = "${ota.vehicle-projection.reconcile.cron:0 0 3 * * ?}")
    public void reconcile() {
        log.info("开始车辆投影周期性对账...");
        try {
            VehicleProjectionSyncService.ProjectionSyncStats stats = vehicleProjectionSyncService.reconcile();
            log.info("车辆投影对账完成: scan={}, add={}, update={}, ignore={}, fail={}, maxVersionDiff={}, 耗时={}ms",
                    stats.getScan(), stats.getAdded(), stats.getUpdated(), stats.getIgnored(),
                    stats.getFailed(), stats.getMaxVersionDiff(), stats.getSyncDelayMs());
        } catch (Exception e) {
            log.error("车辆投影对账执行失败: {}", e.getMessage(), e);
        }
    }
}
