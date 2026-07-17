package net.hwyz.iov.cloud.iov.ota.service.infrastructure.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehStatusMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleProjectionRepository;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleExResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 车辆投影Bootstrap启动任务
 * <p>
 * CR-011: 启动时从 tb_veh_status 候选清单逐 VIN 回源，
 * 将车辆主档数据同步到 tb_vehicle_projection。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.vehicle-projection.bootstrap.enabled", havingValue = "true", matchIfMissing = false)
public class VehicleProjectionBootstrapJob {

    private final VehStatusMapper vehStatusMapper;
    private final VehicleProjectionRepository vehicleProjectionRepository;
    private final VmdVehicleService vmdVehicleService;

    /**
     * 应用启动后执行Bootstrap
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("开始车辆投影Bootstrap...");
        long startTime = System.currentTimeMillis();

        try {
            // 从 tb_veh_status 获取候选VIN列表
            List<VehStatusPo> vehStatusList = vehStatusMapper.selectList(null);
            log.info("Bootstrap候选VIN数: {}", vehStatusList.size());

            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (VehStatusPo vehStatus : vehStatusList) {
                String vin = vehStatus.getVin();
                try {
                    // 检查投影是否已存在
                    if (vehicleProjectionRepository.findByVin(vin).isPresent()) {
                        skipCount++;
                        continue;
                    }

                    // 调用VMD查询车辆信息
                    VehicleExResponse vehicleInfo = vmdVehicleService.getByVin(vin);
                    if (vehicleInfo == null) {
                        log.warn("VMD未找到车辆信息: vin={}", vin);
                        failCount++;
                        continue;
                    }

                    // 构造投影（VMD API当前仅返回vin，其他字段需从其他来源获取）
                    VehicleProjectionPo po = VehicleProjectionPo.builder()
                            .vin(vin)
                            .productionTime(new Date())
                            .plantCode(null)
                            .brandCode(null)
                            .platformCode(null)
                            .carLineCode(null)
                            .modelCode(null)
                            .variantCode(null)
                            .configurationCode(null)
                            .sourceEventId("BOOTSTRAP-" + System.currentTimeMillis())
                            .sourceVersion(System.currentTimeMillis())
                            .sourceEventTime(new Date())
                            .lastSyncTime(new Date())
                            .createTime(new Date())
                            .modifyTime(new Date())
                            .build();

                    vehicleProjectionRepository.insert(po);
                    successCount++;
                    log.debug("Bootstrap同步车辆投影成功: vin={}", vin);
                } catch (Exception e) {
                    failCount++;
                    log.warn("Bootstrap同步车辆投影失败: vin={}, error={}", vin, e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("车辆投影Bootstrap完成: 总数={}, 成功={}, 跳过={}, 失败={}, 耗时={}ms",
                    vehStatusList.size(), successCount, skipCount, failCount, duration);
        } catch (Exception e) {
            log.error("车辆投影Bootstrap执行失败: {}", e.getMessage(), e);
        }
    }
}
