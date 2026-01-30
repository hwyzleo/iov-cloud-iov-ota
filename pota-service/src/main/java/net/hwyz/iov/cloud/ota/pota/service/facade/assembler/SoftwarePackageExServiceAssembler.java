package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwarePackageExService;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwarePackagePo;
import org.mapstruct.Mapper;
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
     * 数据对象转数据传输对象
     *
     * @param softwarePackagePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    SoftwarePackageExService fromPo(SoftwarePackagePo softwarePackagePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param softwarePackageExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SoftwarePackagePo toPo(SoftwarePackageExService softwarePackageExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwarePackagePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SoftwarePackageExService> fromPoList(List<SoftwarePackagePo> softwarePackagePoList);

}
