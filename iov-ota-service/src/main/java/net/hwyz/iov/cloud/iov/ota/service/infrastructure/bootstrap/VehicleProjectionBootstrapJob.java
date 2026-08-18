package net.hwyz.iov.cloud.iov.ota.service.infrastructure.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleProjectionSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 车辆投影 Bootstrap 启动任务（CR-011 / CR-015 §4.3）
 * <p>从 checkpoint 断点分页拉取 VMD 游标快照，幂等 upsert 到 tb_vehicle_projection；
 * 可与 Kafka Consumer 并行，最终以较大 source_version 胜出。</p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.vehicle-projection.bootstrap.enabled", havingValue = "true", matchIfMissing = false)
public class VehicleProjectionBootstrapJob {

    private final VehicleProjectionSyncService vehicleProjectionSyncService;

    /**
     * 应用启动后执行 Bootstrap（游标快照分页）
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("开始车辆投影 Bootstrap（游标快照分页）...");
        try {
            VehicleProjectionSyncService.ProjectionSyncStats stats = vehicleProjectionSyncService.bootstrapFromSnapshot();
            log.info("车辆投影 Bootstrap 完成: scan={}, add={}, update={}, ignore={}, fail={}, 耗时={}ms",
                    stats.getScan(), stats.getAdded(), stats.getUpdated(), stats.getIgnored(),
                    stats.getFailed(), stats.getSyncDelayMs());
        } catch (Exception e) {
            log.error("车辆投影 Bootstrap 执行失败: {}", e.getMessage(), e);
        }
    }
}
