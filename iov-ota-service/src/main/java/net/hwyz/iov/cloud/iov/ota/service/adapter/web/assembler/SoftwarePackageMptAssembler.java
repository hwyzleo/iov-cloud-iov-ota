package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwarePackageMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台软件包信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SoftwarePackageMptAssembler {

    SoftwarePackageMptAssembler INSTANCE = Mappers.getMapper(SoftwarePackageMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param softwarePackagePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    SoftwarePackageMpt fromPo(SoftwarePackagePo softwarePackagePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param softwarePackageMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SoftwarePackagePo toPo(SoftwarePackageMpt softwarePackageMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwarePackagePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SoftwarePackageMpt> fromPoList(List<SoftwarePackagePo> softwarePackagePoList);

}
