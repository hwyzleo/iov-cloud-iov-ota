package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.BomSoftwarePackageOapi;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
     * BOM 向后兼容：packageState 默认 ACTIVE
     */
    @Mappings({
            @Mapping(target = "packageState", constant = "ACTIVE")
    })
    SoftwarePackagePo toPo(BomSoftwarePackageOapi bomSoftwarePackageOapi);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwarePackagePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BomSoftwarePackageOapi> fromPoList(List<SoftwarePackagePo> softwarePackagePoList);

}
