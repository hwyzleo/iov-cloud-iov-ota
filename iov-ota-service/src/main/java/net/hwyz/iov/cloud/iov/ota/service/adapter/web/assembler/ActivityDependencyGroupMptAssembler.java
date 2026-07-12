package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ActivityDependencyGroupMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityDependencyGroupPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ActivityDependencyGroupMptAssembler {

    ActivityDependencyGroupMptAssembler INSTANCE = Mappers.getMapper(ActivityDependencyGroupMptAssembler.class);

    @Mappings({})
    ActivityDependencyGroupMpt fromPo(ActivityDependencyGroupPo po);

    @Mappings({})
    ActivityDependencyGroupPo toPo(ActivityDependencyGroupMpt mpt);

    List<ActivityDependencyGroupMpt> fromPoList(List<ActivityDependencyGroupPo> poList);

}
