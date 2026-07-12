package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.api.vo.BomSoftwarePackageOapi;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.AdaptiveLevel;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwarePackageType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwarePackageState;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.BomSoftwarePackageOapiAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 软件包信息应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwarePackageAppService {

    private final SoftwarePackageMapper softwarePackageMapper;

    /**
     * 查询软件包信息
     *
     * @param deviceCode             设备代码
     * @param softwarePn             软件零件号
     * @param packageCode            软件包代码
     * @param packageName            软件包名称
     * @param softwareBuildVersionId 软件内部版本ID
     * @param beginTime              开始时间
     * @param endTime                结束时间
     * @return 软件包列表
     */
    public List<SoftwarePackagePo> search(String deviceCode, String softwarePn, String packageCode, String packageName,
                                          Long softwareBuildVersionId, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceCode", deviceCode);
        map.put("softwarePn", softwarePn);
        map.put("packageCode", packageCode);
        map.put("packageName", ParamHelper.fuzzyQueryParam(packageName));
        map.put("softwareBuildVersionId", softwareBuildVersionId);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return softwarePackageMapper.selectPoByMap(map);
    }

    /**
     * 列出软件内部版本下的软件包信息
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @return 软件包列表
     */
    public List<SoftwarePackagePo> listBySoftwareBuildVersionId(Long softwareBuildVersionId) {
        Map<String, Object> map = new HashMap<>();
        map.put("softwareBuildVersionId", softwareBuildVersionId);
        return softwarePackageMapper.selectPoByMap(map);
    }

    /**
     * 根据主键ID获取软件包信息
     *
     * @param id 主键ID
     * @return 软件包信息
     */
    public SoftwarePackagePo getSoftwarePackageById(Long id) {
        return softwarePackageMapper.selectPoById(id);
    }

    /**
     * 新增软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    public int createSoftwarePackage(SoftwarePackagePo softwarePackage) {
        applyFieldRules(softwarePackage);
        generatePackageCodeIfAbsent(softwarePackage);
        if (softwarePackage.getPackageState() == null) {
            softwarePackage.setPackageState(SoftwarePackageState.ACTIVE.name());
        }
        return softwarePackageMapper.insertPo(softwarePackage);
    }

    /**
     * 新增BOM软件包信息
     *
     * @param bomSoftwarePackage BOM软件包信息
     * @return 结果
     */
    public SoftwarePackagePo createSoftwarePackage(BomSoftwarePackageOapi bomSoftwarePackage) {
        SoftwarePackagePo softwarePackagePo = BomSoftwarePackageOapiAssembler.INSTANCE.toPo(bomSoftwarePackage);
        applyFieldRules(softwarePackagePo);
        generatePackageCodeIfAbsent(softwarePackagePo);
        softwarePackageMapper.insertPo(softwarePackagePo);
        return softwarePackagePo;
    }

    /**
     * 修改软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    public int modifySoftwarePackage(SoftwarePackagePo softwarePackage) {
        applyFieldRules(softwarePackage);
        softwarePackage.setPackageCode(null);
        return softwarePackageMapper.updatePo(softwarePackage);
    }

    /**
     * 批量删除软件包信息
     *
     * @param ids 软件包信息ID数组
     * @return 结果
     */
    public int deleteSoftwarePackageByIds(Long[] ids) {
        return softwarePackageMapper.batchPhysicalDeletePo(ids);
    }

    // ==================== CR-004: 制品可用性状态流转 ====================

    /**
     * 停用软件包
     */
    public int deprecateSoftwarePackage(Long id) {
        SoftwarePackagePo po = softwarePackageMapper.selectPoById(id);
        if (po == null) {
            throw new IllegalArgumentException("软件包不存在: " + id);
        }
        if (!SoftwarePackageState.ACTIVE.name().equals(po.getPackageState())) {
            throw new IllegalStateException("当前制品状态[" + po.getPackageState() + "]不允许停用");
        }
        po.setPackageState(SoftwarePackageState.DEPRECATED.name());
        return softwarePackageMapper.updatePo(po);
    }

    /**
     * 吊销软件包
     */
    public int revokeSoftwarePackage(Long id) {
        SoftwarePackagePo po = softwarePackageMapper.selectPoById(id);
        if (po == null) {
            throw new IllegalArgumentException("软件包不存在: " + id);
        }
        if (!SoftwarePackageState.ACTIVE.name().equals(po.getPackageState())
                && !SoftwarePackageState.DEPRECATED.name().equals(po.getPackageState())) {
            throw new IllegalStateException("当前制品状态[" + po.getPackageState() + "]不允许吊销");
        }
        po.setPackageState(SoftwarePackageState.REVOKED.name());
        return softwarePackageMapper.updatePo(po);
    }

    /**
     * 退役软件包
     */
    public int retireSoftwarePackage(Long id) {
        SoftwarePackagePo po = softwarePackageMapper.selectPoById(id);
        if (po == null) {
            throw new IllegalArgumentException("软件包不存在: " + id);
        }
        if (!SoftwarePackageState.DEPRECATED.name().equals(po.getPackageState())
                && !SoftwarePackageState.REVOKED.name().equals(po.getPackageState())) {
            throw new IllegalStateException("当前制品状态[" + po.getPackageState() + "]不允许退役");
        }
        po.setPackageState(SoftwarePackageState.RETIRED.name());
        return softwarePackageMapper.updatePo(po);
    }

    /**
     * 生成软件包代码（系统生成·唯一·不可变）
     * 格式：{device_code}-{software_pn}-{FULL/DELTA}-{8位UUID}
     */
    private void generatePackageCodeIfAbsent(SoftwarePackagePo softwarePackage) {
        if (softwarePackage.getPackageCode() == null || softwarePackage.getPackageCode().isBlank()) {
            String deviceCode = softwarePackage.getDeviceCode() != null ? softwarePackage.getDeviceCode() : "UNKNOWN";
            String softwarePn = softwarePackage.getSoftwarePn() != null ? softwarePackage.getSoftwarePn() : "UNKNOWN";
            String packageType = softwarePackage.getPackageType() != null ? softwarePackage.getPackageType() : "UNKNOWN";
            String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            softwarePackage.setPackageCode(deviceCode + "-" + softwarePn + "-" + packageType + "-" + shortUuid);
        }
    }

    /**
     * 应用字段规则（DSN-CR-003 §3.8）
     * - base_software_pn / base_software_ver：仅 DELTA 必填，FULL 时清空
     * - package_adaptive_level：DELTA 必填，FULL 默认 LE(1)
     */
    private void applyFieldRules(SoftwarePackagePo softwarePackage) {
        String packageType = softwarePackage.getPackageType();
        if (SoftwarePackageType.DELTA.name().equals(packageType)) {
            if (softwarePackage.getBaseSoftwarePn() == null || softwarePackage.getBaseSoftwarePn().isBlank()) {
                throw new IllegalArgumentException("DELTA包的基础软件零件号(base_software_pn)不能为空");
            }
            if (softwarePackage.getBaseSoftwareVer() == null || softwarePackage.getBaseSoftwareVer().isBlank()) {
                throw new IllegalArgumentException("DELTA包的基础软件版本(base_software_ver)不能为空");
            }
            if (softwarePackage.getPackageAdaptiveLevel() == null) {
                throw new IllegalArgumentException("DELTA包的适配级别(package_adaptive_level)不能为空");
            }
        } else if (SoftwarePackageType.FULL.name().equals(packageType)) {
            softwarePackage.setBaseSoftwarePn(null);
            softwarePackage.setBaseSoftwareVer(null);
            if (softwarePackage.getPackageAdaptiveLevel() == null) {
                softwarePackage.setPackageAdaptiveLevel(AdaptiveLevel.LE.value);
            }
        }
    }

}
