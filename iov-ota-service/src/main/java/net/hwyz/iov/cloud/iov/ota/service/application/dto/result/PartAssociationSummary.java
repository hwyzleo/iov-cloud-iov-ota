package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ECU 零件关联结果（IOV-OTA-DSN-CR-023 §4.3 / §5）
 *
 * <p>关联键：inventory.ecuId = tb_vehicle_part.deviceCode = VMD vehicleNodeCode。
 * 候选数 0 → NOT_FOUND，1 → MATCHED，>1 → AMBIGUOUS；AMBIGUOUS 时不得任取一条。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartAssociationSummary {

    public static final String STATUS_MATCHED = "MATCHED";
    public static final String STATUS_NOT_FOUND = "NOT_FOUND";
    public static final String STATUS_AMBIGUOUS = "AMBIGUOUS";

    /** MATCHED / NOT_FOUND / AMBIGUOUS */
    private String status;

    /** 该 ECU 对应的有效零件候选数量 */
    private Integer candidateCount;

    /** 仅 MATCHED 时返回零件投影，其余状态为 null */
    private VehiclePartSummary part;
}
