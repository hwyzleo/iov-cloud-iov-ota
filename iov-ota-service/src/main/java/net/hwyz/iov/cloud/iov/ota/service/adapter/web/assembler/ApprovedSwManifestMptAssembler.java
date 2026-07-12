package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ApprovedSwManifestMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ApprovedSwManifestMptAssembler {

    ApprovedSwManifestMptAssembler INSTANCE = Mappers.getMapper(ApprovedSwManifestMptAssembler.class);

    @Mappings({})
    ApprovedSwManifestMpt fromPo(ApprovedSwManifestPo po);

    List<ApprovedSwManifestMpt> fromPoList(List<ApprovedSwManifestPo> poList);

}
