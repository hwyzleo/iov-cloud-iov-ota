package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ActivityTargetVersionMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityTargetVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ActivityTargetVersionMptAssembler {

    ActivityTargetVersionMptAssembler INSTANCE = Mappers.getMapper(ActivityTargetVersionMptAssembler.class);

    @Mappings({})
    ActivityTargetVersionMpt fromPo(ActivityTargetVersionPo po);

    @Mappings({})
    ActivityTargetVersionPo toPo(ActivityTargetVersionMpt mpt);

    List<ActivityTargetVersionMpt> fromPoList(List<ActivityTargetVersionPo> poList);

}
