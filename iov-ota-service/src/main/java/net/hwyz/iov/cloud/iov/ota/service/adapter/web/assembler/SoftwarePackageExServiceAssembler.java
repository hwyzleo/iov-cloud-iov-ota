package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwarePackageExService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
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
