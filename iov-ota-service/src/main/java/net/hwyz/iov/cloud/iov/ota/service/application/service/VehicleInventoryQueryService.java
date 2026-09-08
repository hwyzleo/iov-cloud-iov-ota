package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.InventoryProcessSummary;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventorySoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventorySoftwareUnitPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆清单查询应用服务（CR-019 §6.4 Inventory Query Assembler）
 *
 * <p>组装 ECU → SoftwareUnits 查询模型，支持逐 Target/Slot 导出。
 * legacy SINGLE_IMAGE 通过 synthetic ECU_IMAGE unit 返回；MULTI_TARGET
 * 不允许任取第一项降级。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleInventoryQueryService {

    private final VehicleInventoryMapper vehicleInventoryMapper;
    private final VehicleInventoryItemMapper vehicleInventoryItemMapper;
    private final VehicleInventorySoftwareUnitMapper vehicleInventorySoftwareUnitMapper;

    /**
     * 查询某 VIN 最新已接受清单（含 ECU → SoftwareUnits 明细）。
     */
    public InventoryProcessSummary getLatestByVin(String vin) {
        VehicleInventoryPo inventory = vehicleInventoryMapper.selectLatestByVin(vin);
        if (inventory == null) {
            return null;
        }
        List<VehicleInventoryItemPo> itemPos = vehicleInventoryItemMapper
                .selectByInventoryId(inventory.getId());
        return toSummary(inventory, itemPos);
    }

    /**
     * 导出行：每行一个 VIN + ECU + Target + Slot（CR-019 §10）。
     */
    public List<ExportRow> exportByVin(String vin) {
        VehicleInventoryPo inventory = vehicleInventoryMapper.selectLatestByVin(vin);
        if (inventory == null) {
            return List.of();
        }
        List<VehicleInventoryItemPo> itemPos = vehicleInventoryItemMapper
                .selectByInventoryId(inventory.getId());
        return itemPos.stream()
                .flatMap(item -> vehicleInventorySoftwareUnitMapper
                        .selectByInventoryItemId(item.getId()).stream()
                        .map(u -> ExportRow.builder()
                                .vin(vin)
                                .ecuId(item.getEcuId())
                                .hardwarePn(item.getHardwarePn())
                                .hardwareVersion(item.getHardwareVersion())
                                .softwareModel(item.getSoftwareModel())
                                .softwareTargetCode(u.getSoftwareTargetCode())
                                .softwarePartNumber(u.getSoftwarePartNumber())
                                .swVersion(u.getSwVersion())
                                .slot(u.getSlot())
                                .active(u.getActive())
                                .build()))
                .toList();
    }

    private InventoryProcessSummary toSummary(VehicleInventoryPo inventory,
                                              List<VehicleInventoryItemPo> itemPos) {
        int ecuCount = itemPos.size();
        List<InventoryProcessSummary.EcuInventorySummary> ecuList = itemPos.stream()
                .map(item -> {
                    List<VehicleInventorySoftwareUnitPo> unitPos =
                            vehicleInventorySoftwareUnitMapper.selectByInventoryItemId(item.getId());
                    List<InventoryProcessSummary.SoftwareUnitSummary> units = unitPos.stream()
                            .map(u -> InventoryProcessSummary.SoftwareUnitSummary.builder()
                                    .softwareTargetCode(u.getSoftwareTargetCode())
                                    .softwarePartNumber(u.getSoftwarePartNumber())
                                    .swVersion(u.getSwVersion())
                                    .slot(u.getSlot())
                                    .active(u.getActive())
                                    .digest(u.getDigest())
                                    .build())
                            .toList();
                    return InventoryProcessSummary.EcuInventorySummary.builder()
                            .ecuId(item.getEcuId())
                            .hardwarePn(item.getHardwarePn())
                            .hardwareVersion(item.getHardwareVersion())
                            .softwareModel(item.getSoftwareModel())
                            .softwareUnits(units)
                            .build();
                })
                .toList();
        boolean multi = itemPos.stream()
                .anyMatch(i -> "MULTI_TARGET".equals(i.getSoftwareModel()));
        return InventoryProcessSummary.builder()
                .inventoryRevision(inventory.getInventoryRevision())
                .digest(inventory.getDigest())
                .algorithm(inventory.getAlgorithm())
                .acceptedTime(inventory.getAcceptedTime() == null ? null
                        : inventory.getAcceptedTime().toInstant())
                .ecuCount(ecuCount)
                .inventoryModel(multi ? "MULTI_TARGET" : "SINGLE_IMAGE")
                .canonicalizationVersion(inventory.getCanonicalizationVersion())
                .canonicalDigestHex(inventory.getCanonicalDigest() == null ? null
                        : java.util.HexFormat.of().formatHex(inventory.getCanonicalDigest()))
                .sourceCollectedAt(inventory.getSourceCollectedAt() == null ? null
                        : inventory.getSourceCollectedAt().toInstant())
                .ecuList(ecuList)
                .build();
    }

    /**
     * 导出行（CSV 每行一个 VIN + ECU + Target + Slot）。
     */
    @lombok.Builder
    @lombok.Data
    public static class ExportRow {
        private String vin;
        private String ecuId;
        private String hardwarePn;
        private String hardwareVersion;
        private String softwareModel;
        private String softwareTargetCode;
        private String softwarePartNumber;
        private String swVersion;
        private String slot;
        private Boolean active;
    }
}
