package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

/**
 * VAGW 技术投递观测摘要（CR-015 §3.3 deliveryObservationSummary）
 * <p>独立技术维度，只记录 VIN hash/脱敏，不计入业务成功率。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class DeliveryObservationProcessView {

    private String stage;
    private String outcome;
    private String reason;
    private Boolean retryable;
    private Long retryAfterMs;
    private Long occurredAtMs;
}
