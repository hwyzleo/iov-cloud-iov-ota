package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ActivityInstallOrderMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityInstallOrderPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ActivityInstallOrderMptAssembler {

    ActivityInstallOrderMptAssembler INSTANCE = Mappers.getMapper(ActivityInstallOrderMptAssembler.class);

    @Mappings({})
    ActivityInstallOrderMpt fromPo(ActivityInstallOrderPo po);

    @Mappings({})
    ActivityInstallOrderPo toPo(ActivityInstallOrderMpt mpt);

    List<ActivityInstallOrderMpt> fromPoList(List<ActivityInstallOrderPo> poList);

}
