package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 车辆 ECU 清单头实体（CR-012 §3、§5.1）
 *
 * <p>同 VIN 当前已接受的完整 ECU 清单头；UK(vin, inventoryRevision)。
 * DIGEST 必须命中同 VIN、同 revision 的完整清单，禁止仅凭摘要创建空清单。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class VehicleInventory {

    private final Long id;
    private final String vin;
    /** 清单版本号 */
    private final Long inventoryRevision;
    /** 清单摘要 */
    private final String digest;
    /** 摘要算法 */
    private final String algorithm;
    /** 清单明细 */
    private final List<VehicleInventoryItem> items;
}
