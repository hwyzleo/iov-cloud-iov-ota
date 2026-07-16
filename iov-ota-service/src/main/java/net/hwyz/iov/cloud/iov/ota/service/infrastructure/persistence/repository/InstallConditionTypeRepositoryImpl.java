package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.InstallConditionType;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.InstallConditionTypeRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.InstallConditionTypeMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.InstallConditionTypePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 安装条件类型仓储实现
 */
@Repository
@RequiredArgsConstructor
public class InstallConditionTypeRepositoryImpl implements InstallConditionTypeRepository {
    
    private final InstallConditionTypeMapper mapper;
    
    @Override
    public Optional<InstallConditionType> getById(Long id) {
        InstallConditionTypePo po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public Optional<InstallConditionType> getByCode(String code) {
        QueryWrapper<InstallConditionTypePo> query = new QueryWrapper<>();
        query.eq("code", code).eq("row_valid", 1);
        InstallConditionTypePo po = mapper.selectOne(query);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public List<InstallConditionType> listAll() {
        QueryWrapper<InstallConditionTypePo> query = new QueryWrapper<>();
        query.eq("row_valid", 1).orderByAsc("code");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<InstallConditionType> listByPhase(String phase) {
        QueryWrapper<InstallConditionTypePo> query = new QueryWrapper<>();
        query.eq("row_valid", 1)
             .and(w -> w.isNull("applicable_phase")
                       .or()
                       .like("applicable_phase", phase))
             .orderByAsc("code");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<InstallConditionType> listMandatory() {
        QueryWrapper<InstallConditionTypePo> query = new QueryWrapper<>();
        query.eq("row_valid", 1).eq("mandatory", 1).orderByAsc("code");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public InstallConditionType save(InstallConditionType entity) {
        InstallConditionTypePo po = toPo(entity);
        if (po.getId() == null) {
            mapper.insert(po);
            entity.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return entity;
    }
    
    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
    
    private InstallConditionType toDomain(InstallConditionTypePo po) {
        if (po == null) {
            return null;
        }
        return InstallConditionType.builder()
                .id(po.getId())
                .code(po.getCode())
                .name(po.getName())
                .unit(po.getUnit())
                .valueType(po.getValueType())
                .defaultValue(po.getDefaultValue())
                .applicablePhase(po.getApplicablePhase())
                .mandatory(po.getMandatory())
                .description(po.getDescription())
                .build();
    }
    
    private InstallConditionTypePo toPo(InstallConditionType domain) {
        if (domain == null) {
            return null;
        }
        return InstallConditionTypePo.builder()
                .id(domain.getId())
                .code(domain.getCode())
                .name(domain.getName())
                .unit(domain.getUnit())
                .valueType(domain.getValueType())
                .defaultValue(domain.getDefaultValue())
                .applicablePhase(domain.getApplicablePhase())
                .mandatory(domain.getMandatory())
                .description(domain.getDescription())
                .build();
    }
}
