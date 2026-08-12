package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * 车辆 ECU 清单明细实体（CR-012 §3、§5.1）
 *
 * <p>UK(inventory_id, ecu_id)。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class VehicleInventoryItem {

    private final Long id;
    private final Long inventoryId;
    /** ECU 标识 */
    private final String ecuId;
    /** ECU 名称 */
    private final String ecuName;
    /** 软件零件号 */
    private final String softwarePn;
    /** 软件版本 */
    private final String softwareVersion;
    /** 硬件零件号 */
    private final String hardwarePn;
    /** 硬件版本 */
    private final String hardwareVersion;
}
