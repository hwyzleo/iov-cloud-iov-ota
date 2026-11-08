package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.InventoryProcessSummary;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.PartAssociationSummary;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehiclePartSummary;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆设备软件清单查询服务（IOV-OTA-DSN-CR-023 §2 / §4.3 / §5）
 *
 * <p>以最新已接受的 OTA Canonical Inventory 为设备展示主线，追加 VMD 零件投影关联：
 * <ul>
 *   <li>关联键固定为相同 VIN 下 inventory.ecuId = vehicle_part.deviceCode；</li>
 *   <li>一次按 VIN 批量查询零件，按 deviceCode 内存分组，查询次数与 ECU 数量无关；</li>
 *   <li>候选 0 → NOT_FOUND，1 → MATCHED，>1 → AMBIGUOUS；不 LIMIT 1、不按
 *       hardwarePn / softwarePn / 版本模糊兜底；</li>
 *   <li>清单不存在时沿用既有空语义，不以 VMD 零件伪造 OTA 清单。</li>
 * </ul>
 *
 * <p>该服务独立于 {@link VehicleInventoryQueryService}，确保 TaskVehicle 过程查询
 * 复用 InventoryProcessSummary 时不会无条件加载零件。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleDeviceInventoryQueryService {

    private final VehicleInventoryQueryService vehicleInventoryQueryService;
    private final VehiclePartMapper vehiclePartMapper;

    /**
     * 查询某 VIN 最新已接受清单，并追加每个 ECU 的零件关联结果。
     *
     * @param vin 车架号
     * @return 清单摘要（含 partAssociation），无已接受清单返回 null（兼容既有语义）
     */
    public InventoryProcessSummary getLatestByVin(String vin) {
        InventoryProcessSummary summary = vehicleInventoryQueryService.getLatestByVin(vin);
        if (summary == null || summary.getEcuList() == null || summary.getEcuList().isEmpty()) {
            return summary;
        }
        Map<String, List<VehiclePartPo>> partsByDeviceCode = loadPartsGroupedByDeviceCode(vin);
        for (InventoryProcessSummary.EcuInventorySummary ecu : summary.getEcuList()) {
            ecu.setPartAssociation(resolveAssociation(ecu.getEcuId(), partsByDeviceCode));
        }
        return summary;
    }

    /**
     * 一次按 VIN 批量查询有效零件并按 deviceCode 分组（CR-023 §5.1）。
     */
    private Map<String, List<VehiclePartPo>> loadPartsGroupedByDeviceCode(String vin) {
        List<VehiclePartPo> parts = vehiclePartMapper.selectByVin(vin);
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<VehiclePartPo>> grouped = new HashMap<>();
        for (VehiclePartPo part : parts) {
            if (part == null || part.getDeviceCode() == null) {
                continue;
            }
            grouped.computeIfAbsent(part.getDeviceCode(), k -> new ArrayList<>()).add(part);
        }
        return grouped;
    }

    /**
     * 唯一性解析（CR-023 §5.2 ~ §5.4）：精确匹配 ecuId = deviceCode，多候选不任取。
     */
    private PartAssociationSummary resolveAssociation(
            String ecuId, Map<String, List<VehiclePartPo>> partsByDeviceCode) {
        if (ecuId == null || ecuId.isEmpty()) {
            return PartAssociationSummary.builder()
                    .status(PartAssociationSummary.STATUS_NOT_FOUND)
                    .candidateCount(0)
                    .part(null)
                    .build();
        }
        List<VehiclePartPo> candidates = partsByDeviceCode.get(ecuId);
        int candidateCount = candidates == null ? 0 : candidates.size();
        if (candidateCount == 0) {
            return PartAssociationSummary.builder()
                    .status(PartAssociationSummary.STATUS_NOT_FOUND)
                    .candidateCount(0)
                    .part(null)
                    .build();
        }
        if (candidateCount > 1) {
            return PartAssociationSummary.builder()
                    .status(PartAssociationSummary.STATUS_AMBIGUOUS)
                    .candidateCount(candidateCount)
                    .part(null)
                    .build();
        }
        return PartAssociationSummary.builder()
                .status(PartAssociationSummary.STATUS_MATCHED)
                .candidateCount(1)
                .part(toPartSummary(candidates.get(0)))
                .build();
    }

    private VehiclePartSummary toPartSummary(VehiclePartPo po) {
        return VehiclePartSummary.builder()
                .id(po.getId())
                .pn(po.getPn())
                .deviceCode(po.getDeviceCode())
                .deviceItem(po.getDeviceItem())
                .sn(po.getSn())
                .configWord(po.getConfigWord())
                .supplierCode(po.getSupplierCode())
                .batchNum(po.getBatchNum())
                .hardwareVer(po.getHardwareVer())
                .softwareVer(po.getSoftwareVer())
                .hardwarePn(po.getHardwarePn())
                .softwarePn(po.getSoftwarePn())
                .extra(po.getExtra())
                .modifyTime(po.getModifyTime())
                .build();
    }
}
