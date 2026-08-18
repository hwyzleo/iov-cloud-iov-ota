package net.hwyz.iov.cloud.iov.ota.service.domain.exception;

/**
 * 车辆投影回源不存在异常（CR-015 §4.2）
 * <p>VMD 明确返回不存在时抛出；区别于服务不可用（{@link RetryableProjectionException}）。</p>
 *
 * @author hwyz_leo
 */
public class VehicleProjectionNotFoundException extends RuntimeException {

    public VehicleProjectionNotFoundException(String vin) {
        super("VMD 不存在车辆[" + vin + "]");
    }
}
