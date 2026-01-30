package net.hwyz.iov.cloud.ota.pota.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomConfigWordDependencyOapi;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomSoftwareBuildVersionDependencyOapi;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomSoftwareBuildVersionOapi;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomSoftwarePackageOapi;
import net.hwyz.iov.cloud.ota.pota.api.contract.enums.ConfigWordType;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.BomConfigWordDependencyOapiAssembler;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.BomSoftwareBuildVersionDependencyOapiAssembler;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.BomSoftwareBuildVersionOapiAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.SoftwareBuildVersionDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.SoftwareBuildVersionDependencyDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.SoftwareBuildVersionPackageDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 软件内部版本信息应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwareBuildVersionAppService {

    private final ConfigWordAppService configWordAppService;
    private final SoftwareBuildVersionDao softwareBuildVersionDao;
    private final SoftwarePackageAppService softwarePackageAppService;
    private final SoftwareBuildVersionPackageDao softwareBuildVersionPackageDao;
    private final SoftwareBuildVersionDependencyDao softwareBuildVersionDependencyDao;

    /**
     * 查询软件零件版本信息
     *
     * @param key 关键词
     * @return 软件零件版本列表
     */
    public List<SoftwareBuildVersionPo> search(String key) {
        Map<String, Object> map = new HashMap<>();
        map.put("key", key);
        return softwareBuildVersionDao.selectPoByMap(map);
    }

    /**
     * 查询软件零件版本信息
     *
     * @param deviceCode   设备代码
     * @param softwarePn   软件零件号
     * @param baselineCode 基线代码
     * @param beginTime    开始时间
     * @param endTime      结束时间
     * @return 软件零件版本列表
     */
    public List<SoftwareBuildVersionPo> search(String deviceCode, String softwarePn, String baselineCode, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceCode", deviceCode);
        map.put("softwarePn", softwarePn);
        map.put("baselineCode", baselineCode);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return softwareBuildVersionDao.selectPoByMap(map);
    }

    /**
     * 根据基线代码查询软件零件版本信息
     *
     * @param baselineCode 基线代码
     * @return 软件零件版本列表
     */
    public List<SoftwareBuildVersionPo> listByBaselineCode(String baselineCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("baselineCode", baselineCode);
        return softwareBuildVersionDao.selectPoByMap(map);
    }

    /**
     * 列出软件内部版本依赖信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     * @return 软件内部版本依赖信息
     */
    public List<SoftwareBuildVersionDependencyPo> listDependency(Long softwareBuildVersionId) {
        return softwareBuildVersionDependencyDao.selectPoBySoftwareBuildVersionId(softwareBuildVersionId);
    }

    /**
     * 列出软件内部版本配置信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     * @return 软件内部版本配置信息
     */
    public List<ConfigWordPo> listConfigWord(Long softwareBuildVersionId) {
        return configWordAppService.listConfigWordByTypeAndReferenceId(ConfigWordType.SOFTWARE_PART_VERSION, softwareBuildVersionId);
    }

    /**
     * 检查ECU代码及软件零件号及版本是否唯一
     *
     * @param softwarePartVersionId 软件零件版本信息ID
     * @param deviceCode            设备代码
     * @param softwarePn            软件零件号
     * @param softwareBuildVer      软件内部版本
     * @return 结果
     */
    public Boolean checkDeviceCodeAndSoftwarePnUnique(Long softwarePartVersionId, String deviceCode, String softwarePn,
                                                      String softwareBuildVer) {
        if (ObjUtil.isNull(softwarePartVersionId)) {
            softwarePartVersionId = -1L;
        }
        SoftwareBuildVersionPo softwareBuildVersionPo = getSoftwareBuildVersionByDeviceCodeAndSoftwarePnAndVersion(deviceCode,
                softwarePn, softwareBuildVer);
        return !ObjUtil.isNotNull(softwareBuildVersionPo) || softwareBuildVersionPo.getId().longValue() == softwarePartVersionId.longValue();
    }

    /**
     * 根据主键ID获取软件内部版本信息
     *
     * @param id 主键ID
     * @return 软件内部版本信息
     */
    public SoftwareBuildVersionPo getSoftwareBuildVersionById(Long id) {
        return softwareBuildVersionDao.selectPoById(id);
    }

    /**
     * 根据软件零件号及版本获取软件内部版本信息
     *
     * @param deviceCode       设备代码
     * @param softwarePn       软件零件号
     * @param softwareBuildVer 软件内部版本
     * @return 软件零件版本信息
     */
    public SoftwareBuildVersionPo getSoftwareBuildVersionByDeviceCodeAndSoftwarePnAndVersion(String deviceCode, String softwarePn, String softwareBuildVer) {
        return softwareBuildVersionDao.selectPoByDeviceCodeAndSoftwarePnAndSoftwareBuildVer(deviceCode, softwarePn, softwareBuildVer);
    }

    /**
     * 根据软件内部版本ID与配置字ID获取配置字信息
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param configWordId           配置字ID
     * @return 配置字信息
     */
    public ConfigWordPo getConfigWordById(Long softwareBuildVersionId, Long configWordId) {
        return configWordAppService.getConfigWordById(configWordId);
    }

    /**
     * 新增软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 结果
     */
    public int createSoftwareBuildVersion(SoftwareBuildVersionPo softwareBuildVersion) {
        return softwareBuildVersionDao.insertPo(softwareBuildVersion);
    }

    /**
     * 新增BOM软件内部版本信息列表
     *
     * @param bomSoftwareBuildVersionList BOM软件内部版本信息列表
     */
    public void createSoftwareBuildVersions(List<BomSoftwareBuildVersionOapi> bomSoftwareBuildVersionList) {
        bomSoftwareBuildVersionList.forEach(this::createSoftwareBuildVersion);
    }

    /**
     * 新增BOM软件内部版本信息
     *
     * @param bomSoftwareBuildVersion BOM软件内部版本信息
     * @return 结果
     */
    public SoftwareBuildVersionPo createSoftwareBuildVersion(BomSoftwareBuildVersionOapi bomSoftwareBuildVersion) {
        SoftwareBuildVersionPo softwareBuildVersionPo = getSoftwareBuildVersionByDeviceCodeAndSoftwarePnAndVersion(
                bomSoftwareBuildVersion.getDeviceCode(), bomSoftwareBuildVersion.getSoftwarePn(),
                bomSoftwareBuildVersion.getSoftwareBuildVer());
        if (softwareBuildVersionPo == null) {
            softwareBuildVersionPo = BomSoftwareBuildVersionOapiAssembler.INSTANCE.toPo(bomSoftwareBuildVersion);
            softwareBuildVersionDao.insertPo(softwareBuildVersionPo);
        } else {
            softwareBuildVersionPo.setReleaseDate(bomSoftwareBuildVersion.getReleaseDate());
            softwareBuildVersionDao.updatePo(softwareBuildVersionPo);
        }
        List<BomSoftwarePackageOapi> softwarePackageList = bomSoftwareBuildVersion.getSoftwarePackageList();
        if (!softwarePackageList.isEmpty()) {
            Long[] softwarePackageIds = new Long[softwarePackageList.size()];
            for (int i = 0; i < softwarePackageList.size(); i++) {
                SoftwarePackagePo softwarePackage = softwarePackageAppService.createSoftwarePackage(softwarePackageList.get(i));
                softwarePackageIds[i] = softwarePackage.getId();
            }
            createPackage(softwareBuildVersionPo.getId(), softwarePackageIds);
        }
        List<BomSoftwareBuildVersionDependencyOapi> softwareBuildVersionDependencyList = bomSoftwareBuildVersion.getSoftwareBuildVersionDependencyList();
        if (!softwareBuildVersionDependencyList.isEmpty()) {
            Long[] softwarePartVersionIds = new Long[softwareBuildVersionDependencyList.size()];
            for (int i = 0; i < softwareBuildVersionDependencyList.size(); i++) {
                SoftwareBuildVersionPo softwareBuildVersionDependencyPo = getSoftwareBuildVersionByDeviceCodeAndSoftwarePnAndVersion(
                        softwareBuildVersionDependencyList.get(i).getDeviceCode(),
                        softwareBuildVersionDependencyList.get(i).getSoftwarePn(),
                        softwareBuildVersionDependencyList.get(i).getSoftwareBuildVer());
                if (softwareBuildVersionDependencyPo == null) {
                    softwareBuildVersionDependencyPo = BomSoftwareBuildVersionDependencyOapiAssembler.INSTANCE.toPo(softwareBuildVersionDependencyList.get(i));
                    softwareBuildVersionDao.insertPo(softwareBuildVersionDependencyPo);
                }
                softwarePartVersionIds[i] = softwareBuildVersionDependencyPo.getId();
            }
            createDependency(softwareBuildVersionPo.getId(), softwarePartVersionIds, null);
        }
        List<BomConfigWordDependencyOapi> configWordDependencyList = bomSoftwareBuildVersion.getConfigWordDependencyList();
        if (!configWordDependencyList.isEmpty()) {
            List<ConfigWordPo> configWordList = configWordAppService.listConfigWordByTypeAndReferenceId(ConfigWordType.SOFTWARE_PART_VERSION, softwareBuildVersionPo.getId());
            if (configWordList.isEmpty()) {
                for (BomConfigWordDependencyOapi bomConfigWordDependency : configWordDependencyList) {
                    ConfigWordPo configWordPo = BomConfigWordDependencyOapiAssembler.INSTANCE.toPo(bomConfigWordDependency);
                    configWordPo.setType(ConfigWordType.SOFTWARE_PART_VERSION.value);
                    configWordPo.setReferenceId(softwareBuildVersionPo.getId());
                    configWordAppService.createConfigWord(configWordPo);
                }
            }
        }
        return softwareBuildVersionPo;
    }

    /**
     * 新增软件内部版本关联软件包
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwarePackageIds     软件包ID数组
     * @return 结果
     */
    public int createPackage(Long softwareBuildVersionId, Long[] softwarePackageIds) {
        Set<Long> softwarePackageIdSet = softwareBuildVersionPackageDao.selectPoBySoftwareBuildVersionId(softwareBuildVersionId).stream()
                .map(SoftwareBuildVersionPackagePo::getSoftwarePackageId)
                .collect(Collectors.toSet());
        List<SoftwareBuildVersionPackagePo> list = new ArrayList<>();
        for (Long softwarePackageId : softwarePackageIds) {
            if (!softwarePackageIdSet.contains(softwarePackageId)) {
                list.add(SoftwareBuildVersionPackagePo.builder()
                        .softwareBuildVersionId(softwareBuildVersionId)
                        .softwarePackageId(softwarePackageId)
                        .sort(99)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return softwareBuildVersionPackageDao.batchInsertPo(list);
        }
        return 0;
    }

    /**
     * 新增依赖软件内部版本
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 依赖软件内部版本ID数组
     * @param adaptiveLevel           适配级别
     * @return 结果
     */
    public int createDependency(Long softwareBuildVersionId, Long[] softwareBuildVersionIds, Integer adaptiveLevel) {
        Set<Long> softwareBuildVersionIdSet = softwareBuildVersionDependencyDao.selectPoBySoftwareBuildVersionId(softwareBuildVersionId).stream()
                .map(SoftwareBuildVersionDependencyPo::getDependencySoftwareBuildVersionId)
                .collect(Collectors.toSet());
        List<SoftwareBuildVersionDependencyPo> list = new ArrayList<>();
        for (Long dependencyId : softwareBuildVersionIds) {
            if (!softwareBuildVersionIdSet.contains(dependencyId)) {
                list.add(SoftwareBuildVersionDependencyPo.builder()
                        .softwareBuildVersionId(softwareBuildVersionId)
                        .dependencySoftwareBuildVersionId(dependencyId)
                        .adaptiveLevel(adaptiveLevel)
                        .sort(0)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return softwareBuildVersionDependencyDao.batchInsertPo(list);
        }
        return 0;
    }

    /**
     * 新增配置字
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param configWordPo           配置字
     * @return 结果
     */
    public int createConfigWord(Long softwareBuildVersionId, ConfigWordPo configWordPo) {
        configWordPo.setType(ConfigWordType.SOFTWARE_PART_VERSION.value);
        configWordPo.setReferenceId(softwareBuildVersionId);
        return configWordAppService.createConfigWord(configWordPo);
    }

    /**
     * 修改软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 结果
     */
    public int modifySoftwareBuildVersion(SoftwareBuildVersionPo softwareBuildVersion) {
        return softwareBuildVersionDao.updatePo(softwareBuildVersion);
    }

    /**
     * 修改依赖软件内部版本
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 依赖软件内部版本ID数组
     * @param adaptiveLevel           适配级别
     * @return 结果
     */
    public int modifyDependency(Long softwareBuildVersionId, Long[] softwareBuildVersionIds, Integer adaptiveLevel) {
        AtomicInteger result = new AtomicInteger();
        List<SoftwareBuildVersionDependencyPo> list = softwareBuildVersionDependencyDao.selectPoBySoftwareBuildVersionId(softwareBuildVersionId);
        list.forEach(dependency -> {
            for (Long id : softwareBuildVersionIds) {
                if (dependency.getDependencySoftwareBuildVersionId().longValue() == id) {
                    dependency.setAdaptiveLevel(adaptiveLevel);
                    softwareBuildVersionDependencyDao.updatePo(dependency);
                    result.getAndIncrement();
                }
            }
        });
        return result.get();
    }

    /**
     * 修改软件内部版本配置字
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param configWord             配置字
     * @return 结果
     */
    public int modifyConfigWord(Long softwareBuildVersionId, ConfigWordPo configWord) {
        return configWordAppService.modifyConfigWord(configWord);
    }

    /**
     * 批量删除软件内部版本信息
     *
     * @param ids 软件内部版本信息ID数组
     * @return 结果
     */
    public int deleteSoftwareBuildVersionByIds(Long[] ids) {
        return softwareBuildVersionDao.batchPhysicalDeletePo(ids);
    }

    /**
     * 删除关联的软件包
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwarePackageIds     软件包ID数组
     * @return 结果
     */
    public int deletePackage(Long softwareBuildVersionId, Long[] softwarePackageIds) {
        return softwareBuildVersionPackageDao.batchPhysicalDeletePoBySoftwareBuildVersionIdAndSoftwarePackageIds(softwareBuildVersionId, softwarePackageIds);
    }

    /**
     * 删除关联的软件包
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 依赖的软件内部版本ID数组
     * @return 结果
     */
    public int deleteDependency(Long softwareBuildVersionId, Long[] softwareBuildVersionIds) {
        return softwareBuildVersionDependencyDao.batchPhysicalDeletePoBySoftwareBuildVersionIdAndDependencyIds(softwareBuildVersionId, softwareBuildVersionIds);
    }

    /**
     * 删除软件内部版本下配置字
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param configWordIds          依赖的软件内部版本ID数组
     * @return 结果
     */
    public int deleteConfigWord(Long softwareBuildVersionId, Long[] configWordIds) {
        return configWordAppService.deleteConfigWordByIds(configWordIds);
    }

    /**
     * 统计软件内部版本软件包数量
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @return 软件内部版本软件包数量
     */
    public int countPackage(Long softwareBuildVersionId) {
        return softwareBuildVersionPackageDao.countBySoftwareBuildVersionId(softwareBuildVersionId);
    }

    /**
     * 统计软件内部版本依赖数量
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @return 软件部件版本依赖数量
     */
    public int countDependency(Long softwareBuildVersionId) {
        return softwareBuildVersionDependencyDao.countBySoftwareBuildVersionId(softwareBuildVersionId);
    }

}
