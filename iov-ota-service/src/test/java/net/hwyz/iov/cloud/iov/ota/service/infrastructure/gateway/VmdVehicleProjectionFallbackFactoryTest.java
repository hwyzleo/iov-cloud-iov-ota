package net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway;

import feign.FeignException;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.RetryableProjectionException;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleProjectionNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CR-015 P1-B VMD 网关降级测试：404 → 车辆不存在；其他 → 服务不可用（可重试）
 *
 * @author hwyz_leo
 */
@DisplayName("VmdVehicleProjectionFallbackFactory 降级语义")
class VmdVehicleProjectionFallbackFactoryTest {

    private final VmdVehicleProjectionFallbackFactory factory = new VmdVehicleProjectionFallbackFactory();

    @Test
    @DisplayName("404 -> VehicleProjectionNotFoundException（明确不存在）")
    void notFound_throwsNotFound() {
        FeignException feign404 = mock(FeignException.class);
        when(feign404.status()).thenReturn(404);

        VmdVehicleProjectionGateway gateway = factory.create(feign404);
        assertThrows(VehicleProjectionNotFoundException.class, () -> gateway.getByVin("LSV0000000000000001"));
    }

    @Test
    @DisplayName("其他异常 -> RetryableProjectionException（服务不可用，不得误判为不存在）")
    void unavailable_throwsRetryable() {
        RuntimeException cause = new RuntimeException("connection refused");

        VmdVehicleProjectionGateway gateway = factory.create(cause);
        assertThrows(RetryableProjectionException.class, () -> gateway.getByVin("LSV0000000000000001"));
    }

    @Test
    @DisplayName("快照接口不可用 -> RetryableProjectionException")
    void snapshotUnavailable_throwsRetryable() {
        VmdVehicleProjectionGateway gateway = factory.create(new RuntimeException("timeout"));
        assertThrows(RetryableProjectionException.class,
                () -> gateway.getSnapshot("c1", 100, 0L));
    }
}
