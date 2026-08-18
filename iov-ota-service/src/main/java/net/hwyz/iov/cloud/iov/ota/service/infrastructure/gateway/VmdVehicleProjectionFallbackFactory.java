package net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.RetryableProjectionException;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleProjectionNotFoundException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto.VmdVehicleProjectionDto;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto.VmdVehicleProjectionSnapshotDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * VMD 车辆主档查询网关降级（CR-015 §4.2）
 * <p>区分：404 → 车辆不存在（VehicleProjectionNotFoundException）；
 * 其他异常 → 服务不可用（RetryableProjectionException），不得误判为 NOT_FOUND。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdVehicleProjectionFallbackFactory implements FallbackFactory<VmdVehicleProjectionGateway> {

    @Override
    public VmdVehicleProjectionGateway create(Throwable throwable) {
        return new VmdVehicleProjectionGateway() {
            @Override
            public VmdVehicleProjectionDto getByVin(String vin) {
                if (isNotFound(throwable)) {
                    log.warn("VMD 明确不存在车辆[{}]", vin);
                    throw new VehicleProjectionNotFoundException(vin);
                }
                log.error("VMD 按 VIN 回源服务不可用: vin={}, cause={}", vin, throwable.getMessage(), throwable);
                throw new RetryableProjectionException("VMD 按 VIN 回源服务不可用: " + throwable.getMessage(), throwable);
            }

            @Override
            public VmdVehicleProjectionSnapshotDto getSnapshot(String cursor, Integer size, Long updatedAfter) {
                log.error("VMD 车辆快照服务不可用: cursor={}, cause={}", cursor, throwable.getMessage(), throwable);
                throw new RetryableProjectionException("VMD 车辆快照服务不可用: " + throwable.getMessage(), throwable);
            }
        };
    }

    private boolean isNotFound(Throwable throwable) {
        return throwable instanceof FeignException && ((FeignException) throwable).status() == 404;
    }
}
