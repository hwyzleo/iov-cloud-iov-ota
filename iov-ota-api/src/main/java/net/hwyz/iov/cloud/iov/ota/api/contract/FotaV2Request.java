package net.hwyz.iov.cloud.iov.ota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CCP FOTA v2 公共请求信封（CR-012 §4）
 *
 * <p>所有 /ccp/fota/v2/** 写操作请求统一携带：设备绑定、防重放、协议版本和幂等键。
 *
 * @param <T> 业务请求数据
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotaV2Request<T> {

    /**
     * 协议版本
     */
    private String protocolVersion;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 请求时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 防重放随机数
     */
    private String nonce;

    /**
     * 幂等键（写操作必填）
     */
    private String idempotencyKey;

    /**
     * 业务数据
     */
    private T data;
}
