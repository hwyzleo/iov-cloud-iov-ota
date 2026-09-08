package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Execution 收口后的 ECU 实际版本与结果（CR-015 §3.3 ecuResultSummary / CR-019 §4.23.5）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class EcuResultProcessView {

    private String ecuId;
    /** CR-019：ECU 软件模型 */
    private String softwareModel;
    private String targetSoftwareVersion;
    private String actualSoftwareVersion;
    private String result;
    private String failReason;
    /** CR-019：per-Target/Slot 软件单元结果 */
    private List<SoftwareUnitResultView> softwareUnitResults;

    /**
     * 软件单元结果。
     */
    @Data
    @Builder
    public static class SoftwareUnitResultView {
        private String softwareTargetCode;
        private String sourceVersion;
        private String targetVersion;
        private String actualVersion;
        private String slot;
        private Boolean active;
        private String result;
        private String failureStage;
        private String rollbackResult;
        private String packageId;
    }
}
