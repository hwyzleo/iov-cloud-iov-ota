package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 已接受 ECU 清单摘要（CR-015 §3.3 inventorySummary）
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
}
