package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 规范车辆清单（CR-019 §4.1）
 *
 * <p>所有输入（legacy 单值或 software_units[]）先规范化为本模型，再进入
 * 摘要、幂等、持久化、任务匹配、事件和查询流程。领域层不再分别实现
 * legacy/multi-target 分支。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class CanonicalVehicleInventory {

    /** 车架号 */
    private final String vin;
    /** 清单版本号 */
    private final Long inventoryRevision;
    /** 车端清单采集时间 */
    private final java.time.Instant collectedAt;
    /** canonicalization 版本（1/2） */
    private final int canonicalizationVersion;
    /** 规范 ECU 列表 */
    private final List<CanonicalEcu> ecuList;
}
