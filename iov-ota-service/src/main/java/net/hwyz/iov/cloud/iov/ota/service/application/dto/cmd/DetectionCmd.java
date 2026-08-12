package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务检测命令（CR-012 §5.1、US-074）
 *
 * <p>清单握手（FULL/DIGEST）、任务匹配和本地任务对账。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionCmd {

    /** 车架号 */
    private String vin;

    /** 清单模式：FULL / DIGEST */
    private String inventoryMode;

    /** 清单版本号 */
    private Long inventoryRevision;

    /** 清单摘要（DIGEST 模式） */
    private String inventoryDigest;

    /** 摘要算法 */
    private String digestAlgorithm;

    /** 完整清单明细（FULL 模式） */
    private List<InventoryItemCmd> inventoryItems;

    /** 本地任务版本（车端持有的 taskRevision） */
    private Long localTaskRevision;
}
