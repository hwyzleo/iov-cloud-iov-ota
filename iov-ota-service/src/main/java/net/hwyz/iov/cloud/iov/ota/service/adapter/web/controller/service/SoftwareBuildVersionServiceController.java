package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdPartService;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleNodeService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.PartExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleNodeExResponse;
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionDependencyExService;
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwarePackageAppService;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionDependencyExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwarePackageExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.PartNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.SoftwareBuildVersionNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionDependencyPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
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
@RequestMapping(value = "/api/service/softwareBuildVersion/v1")
public class SoftwareBuildVersionServiceController {

    private final VmdPartService vmdPartService;
    private final VmdVehicleNodeService vmdVehicleNodeService;
    private final SoftwarePackageAppService softwarePackageAppService;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;

    /**
     * 根据软件零件内部信息ID获取软件内部版本信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     */
    @GetMapping(value = "/{softwareBuildVersionId}")
    public SoftwareBuildVersionExService getInfo(@PathVariable Long softwareBuildVersionId) {
        log.info("根据软件内部版本信息ID[{}]获取软件内部版本信息", softwareBuildVersionId);
        SoftwareBuildVersionPo softwareBuildVersionPo = softwareBuildVersionAppService.getSoftwareBuildVersionById(softwareBuildVersionId);
        if (ObjUtil.isNull(softwareBuildVersionPo)) {
            throw new SoftwareBuildVersionNotExistException(softwareBuildVersionId);
        }
        PartExResponse part = vmdPartService.getByCode(softwareBuildVersionPo.getSoftwarePn());
        if (ObjUtil.isNull(part)) {
            throw new PartNotExistException(softwareBuildVersionPo.getSoftwarePn());
        }
        SoftwareBuildVersionExService softwareBuildVersionExService = SoftwareBuildVersionExServiceAssembler.INSTANCE.fromPo(softwareBuildVersionPo);
        softwareBuildVersionExService.setSoftwarePartName(part.getName());
        VehicleNodeExResponse vehicleNode = vmdVehicleNodeService.getByCode(part.getVehicleNodeCode());
        if (vehicleNode != null) {
            softwareBuildVersionExService.setSoftwarePartOta(vehicleNode.getOtaSupport() != null && vehicleNode.getOtaSupport().contains("OTA"));
            softwareBuildVersionExService.setSoftwarePartLockUnlockSecurityComponent(Boolean.TRUE.equals(vehicleNode.getCore()));
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
            exService.setChangeNote(softwareBuildVersion.getChangeNote());
            exService.setSoftwareSource(softwareBuildVersion.getSoftwareSource());
            exService.setReleaseTime(softwareBuildVersion.getReleaseTime());
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
        log.info("根据设备代码[{}]、软件零件号[{}]、软件内部版本[{}]获取软件内部版本信息", deviceCode, softwarePn, softwareBuildVer);
        softwarePn = softwarePn.substring(0, 8);
        SoftwareBuildVersionPo softwareBuildVersion = softwareBuildVersionAppService.getSoftwareBuildVersionByDeviceCodeAndSoftwarePnAndVersion(deviceCode,
                softwarePn, softwareBuildVer);
        return SoftwareBuildVersionExServiceAssembler.INSTANCE.fromPo(softwareBuildVersion);
    }

}
