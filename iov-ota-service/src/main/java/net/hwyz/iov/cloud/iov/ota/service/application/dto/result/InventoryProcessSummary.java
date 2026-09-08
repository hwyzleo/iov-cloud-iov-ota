package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 已接受 ECU 清单摘要（CR-015 §3.3 inventorySummary / CR-019 §4.23.5）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class InventoryProcessSummary {

    private Long inventoryRevision;
    private String digest;
    private String algorithm;
    private Instant acceptedTime;
    private Integer ecuCount;
    /** CR-019：inventory model（SINGLE_IMAGE/MULTI_TARGET） */
    private String inventoryModel;
    /** CR-019：canonicalization 版本 */
    private Integer canonicalizationVersion;
    /** CR-019：canonical digest（小写 hex，v2 时非空） */
    private String canonicalDigestHex;
    /** CR-019：车端采集时间 */
    private Instant sourceCollectedAt;
    /** CR-019：ECU → SoftwareUnit 明细（legacy 单值通过 synthetic ECU_IMAGE 返回） */
    private List<EcuInventorySummary> ecuList;

    /**
     * ECU 清单明细（含规范软件单元）。
     */
    @Data
    @Builder
    public static class EcuInventorySummary {
        private String ecuId;
        private String hardwarePn;
        private String hardwareVersion;
        private String softwareModel;
        private List<SoftwareUnitSummary> softwareUnits;
    }

    /**
     * 规范软件单元。
     */
    @Data
    @Builder
    public static class SoftwareUnitSummary {
        private String softwareTargetCode;
        private String softwarePartNumber;
        private String swVersion;
        private String slot;
        private Boolean active;
        private String digest;
    }
}
