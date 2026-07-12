package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.UserConsentMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UserConsentPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UserConsentMptAssembler {

    UserConsentMptAssembler INSTANCE = Mappers.getMapper(UserConsentMptAssembler.class);

    @Mappings({})
    UserConsentMpt fromPo(UserConsentPo po);

    List<UserConsentMpt> fromPoList(List<UserConsentPo> poList);

}
