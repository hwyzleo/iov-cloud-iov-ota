package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.VehiclePartPoConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartHistoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartHistoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 车辆零件相关应用服务类
 *
 * <p>IOV-OTA-DSN-CR-023 §6：saveVehicleParts 保持 vin + pn 业务键与增量语义，
 * 修复为「全部受管字段差异检测 + 全字段更新 + 同事务历史留痕」。
 *
 * <ul>
 *   <li>不存在 → 插入完整投影 + ADD 历史；</li>
 *   <li>存在且受管字段全等 → 幂等成功，不更新、不写历史；</li>
 *   <li>存在且任一受管字段变化 → 更新全部受管字段 + CHANGE 历史（同一事务）；</li>
 *   <li>NULL 与空串按现有 StrUtil.nullToEmpty 语义视为等价，但比较大小写敏感；</li>
 *   <li>请求中未出现的零件不删除（增量语义，非整车快照）。</li>
 * </ul>
 *
 * <p>日志与历史约束：应用日志不输出完整 VIN 或原始配置字；历史保存变更后完整快照、
 * 请求 remark 与变更字段名称，不伪造未知旧值。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehiclePartAppService {

    /** 受管字段（顺序稳定，用于输出变更字段名称；vin + pn 为身份键不在此列） */
    private static final Map<String, Function<VehiclePartPo, String>> MANAGED_FIELDS;

    static {
        Map<String, Function<VehiclePartPo, String>> fields = new LinkedHashMap<>();
        fields.put("deviceCode", VehiclePartPo::getDeviceCode);
        fields.put("deviceItem", VehiclePartPo::getDeviceItem);
        fields.put("sn", VehiclePartPo::getSn);
        fields.put("configWord", VehiclePartPo::getConfigWord);
        fields.put("supplierCode", VehiclePartPo::getSupplierCode);
        fields.put("batchNum", VehiclePartPo::getBatchNum);
        fields.put("hardwareVer", VehiclePartPo::getHardwareVer);
        fields.put("softwareVer", VehiclePartPo::getSoftwareVer);
        fields.put("hardwarePn", VehiclePartPo::getHardwarePn);
        fields.put("softwarePn", VehiclePartPo::getSoftwarePn);
        fields.put("extra", VehiclePartPo::getExtra);
        MANAGED_FIELDS = Collections.unmodifiableMap(fields);
    }

    private final VehiclePartMapper vehiclePartMapper;
    private final VehiclePartHistoryMapper vehiclePartHistoryMapper;

    /**
     * 保存车辆零件信息（增量 upsert，全受管字段差异更新，与历史同事务）
     *
     * @param vin             车架号
     * @param remark          备注
     * @param vehiclePartList 车辆零件列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveVehicleParts(String vin, String remark, List<VehiclePartPo> vehiclePartList) {
        if (vehiclePartList == null || vehiclePartList.isEmpty()) {
            return;
        }
        for (VehiclePartPo incoming : vehiclePartList) {
            if (incoming == null) {
                continue;
            }
            String pn = incoming.getPn();
            if (StrUtil.isBlank(pn)) {
                // vin + pn 是身份键，缺失时无法 upsert，显式失败避免静默丢数据
                throw new IllegalArgumentException("车辆零件编号(pn)不能为空");
            }
            VehiclePartPo current = vehiclePartMapper.selectPoByVinAndPn(vin, pn);
            if (current == null) {
                insertPart(vin, remark, pn, incoming);
            } else {
                updatePartIfChanged(remark, current, incoming);
            }
        }
    }

    private void insertPart(String vin, String remark, String pn, VehiclePartPo incoming) {
        VehiclePartPo newPart = VehiclePartPo.builder()
                .vin(vin)
                .pn(pn)
                .deviceCode(incoming.getDeviceCode())
                .deviceItem(incoming.getDeviceItem())
                .sn(incoming.getSn())
                .configWord(incoming.getConfigWord())
                .supplierCode(incoming.getSupplierCode())
                .batchNum(incoming.getBatchNum())
                .hardwareVer(incoming.getHardwareVer())
                .softwareVer(incoming.getSoftwareVer())
                .hardwarePn(incoming.getHardwarePn())
                .softwarePn(incoming.getSoftwarePn())
                .extra(incoming.getExtra())
                .build();
        try {
            vehiclePartMapper.insertPo(newPart);
        } catch (DuplicateKeyException e) {
            // 并发「先查后插」被 uk_vehicle_part_vin_pn 拦截，转为变更更新（CR-023 §6，C1）
            VehiclePartPo concurrent = vehiclePartMapper.selectPoByVinAndPn(vin, pn);
            if (concurrent == null) {
                throw e;
            }
            log.info("车辆[{}]零件[{}]并发插入命中唯一键，转为变更更新", maskVin(vin), pn);
            updatePartIfChanged(remark, concurrent, incoming);
            return;
        }
        recordHistory(newPart, StrUtil.nullToEmpty(remark) + "新增");
        log.info("新增车辆零件投影: vin={}, pn={}", maskVin(vin), pn);
    }

    private void updatePartIfChanged(String remark, VehiclePartPo current, VehiclePartPo incoming) {
        List<String> changedFields = changedManagedFields(current, incoming);
        if (changedFields.isEmpty()) {
            log.debug("车辆[{}]零件[{}]受管字段无变化，幂等跳过", maskVin(current.getVin()), current.getPn());
            return;
        }
        // 仅覆盖实际变化字段，其余保留当前投影值：全字段写入但不因 NULL/空串等价波动丢失数据
        for (Map.Entry<String, Function<VehiclePartPo, String>> managed : MANAGED_FIELDS.entrySet()) {
            if (!changedFields.contains(managed.getKey())) {
                continue;
            }
            applyManagedField(current, incoming, managed.getKey());
        }
        vehiclePartMapper.updateManagedFields(current);
        recordHistory(current, StrUtil.nullToEmpty(remark) + "变更:" + String.join(",", changedFields));
        log.info("更新车辆零件投影: vin={}, pn={}, fields={}",
                maskVin(current.getVin()), current.getPn(), changedFields);
    }

    /**
     * 差异比较：NULL 与空串等价（StrUtil.nullToEmpty），大小写敏感（不忽略业务差异）。
     */
    private List<String> changedManagedFields(VehiclePartPo current, VehiclePartPo incoming) {
        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, Function<VehiclePartPo, String>> managed : MANAGED_FIELDS.entrySet()) {
            String currentValue = managed.getValue().apply(current);
            String incomingValue = managed.getValue().apply(incoming);
            if (!StrUtil.nullToEmpty(currentValue).equals(StrUtil.nullToEmpty(incomingValue))) {
                changed.add(managed.getKey());
            }
        }
        return changed;
    }

    private void applyManagedField(VehiclePartPo target, VehiclePartPo source, String field) {
        switch (field) {
            case "deviceCode" -> target.setDeviceCode(source.getDeviceCode());
            case "deviceItem" -> target.setDeviceItem(source.getDeviceItem());
            case "sn" -> target.setSn(source.getSn());
            case "configWord" -> target.setConfigWord(source.getConfigWord());
            case "supplierCode" -> target.setSupplierCode(source.getSupplierCode());
            case "batchNum" -> target.setBatchNum(source.getBatchNum());
            case "hardwareVer" -> target.setHardwareVer(source.getHardwareVer());
            case "softwareVer" -> target.setSoftwareVer(source.getSoftwareVer());
            case "hardwarePn" -> target.setHardwarePn(source.getHardwarePn());
            case "softwarePn" -> target.setSoftwarePn(source.getSoftwarePn());
            case "extra" -> target.setExtra(source.getExtra());
            default -> throw new IllegalStateException("未知受管字段: " + field);
        }
    }

    /**
     * 记录车辆零件信息变更日志（变更后完整快照 + remark + 变更字段名称）
     *
     * @param vehiclePartPo 车辆零件对象（变更后）
     * @param remark        变更备注
     */
    private void recordHistory(VehiclePartPo vehiclePartPo, String remark) {
        VehiclePartHistoryPo history = VehiclePartPoConverter.INSTANCE.toHistory(vehiclePartPo);
        history.setDescription(remark);
        vehiclePartHistoryMapper.insertPo(history);
    }

    /**
     * 脱敏 VIN：日志严禁输出完整 VIN（CR-023 §6.1 / §11.3）
     */
    private static String maskVin(String vin) {
        if (vin == null || vin.length() <= 6) {
            return "***";
        }
        return vin.substring(0, 3) + "****" + vin.substring(vin.length() - 3);
    }
}
