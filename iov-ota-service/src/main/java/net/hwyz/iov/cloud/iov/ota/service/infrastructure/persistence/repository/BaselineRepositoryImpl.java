package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.Baseline;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.BaselineRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.BaselineMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselinePo;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;

/**
 * Baseline Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BaselineRepositoryImpl implements BaselineRepository {

    private final BaselineMapper mapper;

    @Override
    public Optional<Baseline> getById(Long id) {
        BaselinePo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public Optional<Baseline> getByBaselineCode(String baselineCode) {
        LambdaQueryWrapper<BaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaselinePo::getBaselineCode, baselineCode);
        BaselinePo po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public Baseline save(Baseline entity) {
        BaselinePo po = toPo(entity);
        if (entity.getId() == null) {
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

    private Baseline toDomain(BaselinePo po) {
        Baseline domain = new Baseline();
        domain.setId(po.getId());
        domain.setBaselineCode(po.getBaselineCode());
        domain.setName(po.getName());
        domain.setAnchorType(po.getAnchorType());
        domain.setAnchorCode(po.getAnchorCode());
        domain.setBaselineVersion(po.getBaselineVersion());
        domain.setBaselineStatus(po.getBaselineStatus());
        domain.setSource(po.getSource());
        domain.setSyncTime(po.getSyncTime() != null ? po.getSyncTime().toInstant() : null);
        return domain;
    }

    private BaselinePo toPo(Baseline domain) {
        BaselinePo po = new BaselinePo();
        po.setId(domain.getId());
        po.setBaselineCode(domain.getBaselineCode());
        po.setName(domain.getName());
        po.setAnchorType(domain.getAnchorType());
        po.setAnchorCode(domain.getAnchorCode());
        po.setBaselineVersion(domain.getBaselineVersion());
        po.setBaselineStatus(domain.getBaselineStatus());
        po.setSource(domain.getSource());
        po.setSyncTime(domain.getSyncTime() != null ? java.util.Date.from(domain.getSyncTime()) : null);
        return po;
    }
}
