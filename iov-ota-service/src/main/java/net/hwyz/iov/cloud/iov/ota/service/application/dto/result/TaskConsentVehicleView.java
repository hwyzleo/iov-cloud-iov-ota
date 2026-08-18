package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Task 级车辆授权视图（CR-016 §8）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskConsentVehicleView {

    private Long vehicleTaskId;
    private String vinMasked;
    private String consentState;
    private String currentReceiptId;
    private String articleVersion;
    private Instant consentUpdatedAt;
    /** 有效/无效原因；为 null 表示凭据有效 */
    private String invalidReason;
}
