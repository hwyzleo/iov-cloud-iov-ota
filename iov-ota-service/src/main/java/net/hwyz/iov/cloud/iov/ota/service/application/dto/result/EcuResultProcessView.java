package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

/**
 * Execution 收口后的 ECU 实际版本与结果（CR-015 §3.3 ecuResultSummary）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class EcuResultProcessView {

    private String ecuId;
    private String targetSoftwareVersion;
    private String actualSoftwareVersion;
    private String result;
    private String failReason;
}
