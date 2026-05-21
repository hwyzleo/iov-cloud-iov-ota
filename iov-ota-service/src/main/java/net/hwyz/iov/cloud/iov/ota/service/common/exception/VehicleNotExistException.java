package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleNotExistException extends OtaBaseException {

    private static final int ERROR_CODE = 411002;

    public VehicleNotExistException(String vin) {
        super(ERROR_CODE);
        log.warn("车辆[{}]不存在", vin);
    }

}
