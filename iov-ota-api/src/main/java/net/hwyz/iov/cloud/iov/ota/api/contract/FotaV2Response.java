package net.hwyz.iov.cloud.iov.ota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CCP FOTA v2 公共响应信封（CR-012 §4）
 *
 * <p>公共响应的 code=0 仅表示接口受理，业务结果由 data 内的
 * availability/allowed/accepted/resultAccepted 和权威状态判断。
 *
 * @param <T> 业务响应数据
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotaV2Response<T> {

    /**
     * 错误码，"0" 表示受理成功
     */
    private String code;

    /**
     * 描述信息
     */
    private String message;

    /**
     * 请求追踪ID
     */
    private String requestId;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 受理成功响应。
     */
    public static <T> FotaV2Response<T> ok(T data) {
        return FotaV2Response.<T>builder()
                .code("0")
                .message("OK")
                .data(data)
                .build();
    }

    /**
     * 失败响应。
     */
    public static <T> FotaV2Response<T> fail(String code, String message) {
        return FotaV2Response.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
