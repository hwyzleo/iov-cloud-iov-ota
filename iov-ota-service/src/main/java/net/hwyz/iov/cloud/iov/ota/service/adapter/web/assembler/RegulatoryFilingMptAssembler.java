package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.RegulatoryFilingMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.RegulatoryFilingPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface RegulatoryFilingMptAssembler {

    RegulatoryFilingMptAssembler INSTANCE = Mappers.getMapper(RegulatoryFilingMptAssembler.class);

    @Mappings({})
    RegulatoryFilingMpt fromPo(RegulatoryFilingPo po);

    @Mappings({})
    RegulatoryFilingPo toPo(RegulatoryFilingMpt mpt);

    List<RegulatoryFilingMpt> fromPoList(List<RegulatoryFilingPo> poList);

}
