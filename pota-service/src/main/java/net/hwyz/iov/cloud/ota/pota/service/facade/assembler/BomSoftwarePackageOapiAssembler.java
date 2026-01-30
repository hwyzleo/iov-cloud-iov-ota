package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.BomSoftwarePackageOapi;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwarePackagePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 开放平台BOM软件包转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BomSoftwarePackageOapiAssembler {

    BomSoftwarePackageOapiAssembler INSTANCE = Mappers.getMapper(BomSoftwarePackageOapiAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param softwarePackagePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BomSoftwarePackageOapi fromPo(SoftwarePackagePo softwarePackagePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param bomSoftwarePackageOapi 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SoftwarePackagePo toPo(BomSoftwarePackageOapi bomSoftwarePackageOapi);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwarePackagePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BomSoftwarePackageOapi> fromPoList(List<SoftwarePackagePo> softwarePackagePoList);

}
