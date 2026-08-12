package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拒绝/冲突结果负载（CR-013 §5.1）
 *
 * <p>同业务键异摘要（摘要冲突）或业务拒绝时生产的可表达业务拒绝事件。
 *
 * @author hwyz_leo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtaKafkaReject {

    private boolean rejected = true;
    private String code;
    private String reason;

    public static OtaKafkaReject of(String code, String reason) {
        return new OtaKafkaReject(true, code, reason);
    }
}
