package net.hwyz.iov.cloud.ota.pota.service.facade.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionDependencyExService;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.ota.pota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.ota.pota.service.application.service.SoftwarePackageAppService;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.SoftwareBuildVersionDependencyExServiceAssembler;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.SoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.SoftwarePackageExServiceAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.exception.PartNotExistException;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.exception.SoftwareBuildVersionNotExistException;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwareBuildVersionDependencyPo;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.tsp.vmd.api.contract.DeviceExService;
import net.hwyz.iov.cloud.tsp.vmd.api.contract.PartExService;
import net.hwyz.iov.cloud.tsp.vmd.api.feign.service.ExDeviceService;
import net.hwyz.iov.cloud.tsp.vmd.api.feign.service.ExPartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 软件内部版本相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/service/softwareBuildVersion")
public class SoftwareBuildVersionServiceController {

    private final ExPartService exPartService;
    private final ExDeviceService exDeviceService;
    private final SoftwarePackageAppService softwarePackageAppService;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;

    /**
     * 根据软件零件内部信息ID获取软件内部版本信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     */
    @GetMapping(value = "/{softwareBuildVersionId}")
    public SoftwareBuildVersionExService getInfo(@PathVariable Long softwareBuildVersionId) {
        logger.info("根据软件内部版本信息ID[{}]获取软件内部版本信息", softwareBuildVersionId);
        SoftwareBuildVersionPo softwareBuildVersionPo = softwareBuildVersionAppService.getSoftwareBuildVersionById(softwareBuildVersionId);
        if (ObjUtil.isNull(softwareBuildVersionPo)) {
            throw new SoftwareBuildVersionNotExistException(softwareBuildVersionId);
        }
        PartExService part = exPartService.getByPn(softwareBuildVersionPo.getSoftwarePn());
        if (ObjUtil.isNull(part)) {
            throw new PartNotExistException(softwareBuildVersionPo.getSoftwarePn());
        }
        SoftwareBuildVersionExService softwareBuildVersionExService = SoftwareBuildVersionExServiceAssembler.INSTANCE.fromPo(softwareBuildVersionPo);
        softwareBuildVersionExService.setSoftwarePartName(part.getName());
        DeviceExService device = exDeviceService.getByCode(part.getDeviceCode());
        if (device != null) {
            softwareBuildVersionExService.setSoftwarePartOta(device.getOtaSupport().contains("FOTA"));
            softwareBuildVersionExService.setSoftwarePartLockUnlockSecurityComponent(device.getLockUnlockSecurityComponent() > 0);
        }
        List<SoftwarePackagePo> softwarePackageList = softwarePackageAppService.listBySoftwareBuildVersionId(softwareBuildVersionId);
        softwareBuildVersionExService.setSoftwarePackageList(SoftwarePackageExServiceAssembler.INSTANCE.fromPoList(softwarePackageList));
        List<SoftwareBuildVersionDependencyPo> dependencyPoList = softwareBuildVersionAppService.listDependency(softwareBuildVersionId);
        List<SoftwareBuildVersionDependencyExService> dependencyExServiceList = SoftwareBuildVersionDependencyExServiceAssembler.INSTANCE.fromPoList(dependencyPoList);
        dependencyExServiceList.forEach(exService -> {
            SoftwareBuildVersionPo softwareBuildVersion = softwareBuildVersionAppService.getSoftwareBuildVersionById(exService.getDependencySoftwareBuildVersionId());
            exService.setDeviceCode(softwareBuildVersion.getDeviceCode());
            exService.setSoftwarePn(softwareBuildVersion.getSoftwarePn());
            exService.setSoftwareBuildVer(softwareBuildVersion.getSoftwareBuildVer());
            exService.setSoftwareReport(softwareBuildVersion.getSoftwareReport());
            exService.setSoftwareDesc(softwareBuildVersion.getSoftwareDesc());
            exService.setSoftwareSource(softwareBuildVersion.getSoftwareSource());
            exService.setAdaptiveAssemblyPn(softwareBuildVersion.getAdaptiveAssemblyPn());
            exService.setAdaptiveSoftwarePn(softwareBuildVersion.getAdaptiveSoftwarePn());
            exService.setReleaseDate(softwareBuildVersion.getReleaseDate());
            exService.setCreateTime(softwareBuildVersion.getCreateTime());
        });
        softwareBuildVersionExService.setDependencyList(dependencyExServiceList);
        return softwareBuildVersionExService;
    }

    /**
     * 根据ECU代码、软件零件号、软件内部版本获取软件内部版本信息
     *
     * @param deviceCode       设备代码
     * @param softwarePn       软件零件号（包含软件零件版本）
     * @param softwareBuildVer 软件内部版本
     * @return 软件内部版本信息
     */
    @GetMapping(value = "/info")
    public SoftwareBuildVersionExService getInfo(@RequestParam String deviceCode, @RequestParam String softwarePn, @RequestParam String softwareBuildVer) {
        logger.info("根据设备代码[{}]、软件零件号[{}]、软件内部版本[{}]获取软件内部版本信息", deviceCode, softwarePn, softwareBuildVer);
        softwarePn = softwarePn.substring(0, 8);
        SoftwareBuildVersionPo softwareBuildVersion = softwareBuildVersionAppService.getSoftwareBuildVersionByDeviceCodeAndSoftwarePnAndVersion(deviceCode,
                softwarePn, softwareBuildVer);
        return SoftwareBuildVersionExServiceAssembler.INSTANCE.fromPo(softwareBuildVersion);
    }

}
