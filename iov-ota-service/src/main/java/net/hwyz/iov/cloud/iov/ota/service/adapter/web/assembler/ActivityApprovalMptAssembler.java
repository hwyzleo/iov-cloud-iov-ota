package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ActivityApprovalMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityApprovalPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ActivityApprovalMptAssembler {

    ActivityApprovalMptAssembler INSTANCE = Mappers.getMapper(ActivityApprovalMptAssembler.class);

    @Mappings({})
    ActivityApprovalMpt fromPo(ActivityApprovalPo po);

    List<ActivityApprovalMpt> fromPoList(List<ActivityApprovalPo> poList);

}
