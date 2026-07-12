package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwarePackageExService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackageVo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务软件包信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SoftwarePackageExServiceAssembler {

    SoftwarePackageExServiceAssembler INSTANCE = Mappers.getMapper(SoftwarePackageExServiceAssembler.class);

    /**
     * 数据传输对象转值对象
     *
     * @param softwarePackageExService 数据传输对象
     * @return 值对象
     */
    @Mappings({
            @Mapping(target = "packageAdaptiveLevel", expression = "java(net.hwyz.iov.cloud.iov.ota.api.vo.enums.AdaptiveLevel.valOf(softwarePackageExService.getPackageAdaptiveLevel()))")
    })
    SoftwarePackageVo toVo(SoftwarePackageExService softwarePackageExService);

    /**
     * 数据传输对象列表转值对象列表
     *
     * @param softwarePackageExServiceList 数据传输对象列表
     * @return 值对象列表
     */
    List<SoftwarePackageVo> toVoList(List<SoftwarePackageExService> softwarePackageExServiceList);

    @Mappings({
            @Mapping(target = "packageState", expression = "java(softwarePackagePo.getPackageState() == null ? null : net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwarePackageState.valueOf(softwarePackagePo.getPackageState()).value)")
    })
    SoftwarePackageExService fromPo(SoftwarePackagePo softwarePackagePo);

    List<SoftwarePackageExService> fromPoList(List<SoftwarePackagePo> softwarePackagePoList);

}
