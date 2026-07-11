package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.BaselineItem;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.BaselineItemRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.BaselineItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselineItemPo;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BaselineItem Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BaselineItemRepositoryImpl implements BaselineItemRepository {

    private final BaselineItemMapper mapper;

    @Override
    public Optional<BaselineItem> getById(Long id) {
        BaselineItemPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public List<BaselineItem> listByBaselineCode(String baselineCode) {
        LambdaQueryWrapper<BaselineItemPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaselineItemPo::getBaselineCode, baselineCode);
        List<BaselineItemPo> poList = mapper.selectList(wrapper);
        return poList.stream()
                .filter(po -> Boolean.TRUE.equals(po.getRowValid()))
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public BaselineItem save(BaselineItem entity) {
        BaselineItemPo po = toPo(entity);
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

    private BaselineItem toDomain(BaselineItemPo po) {
        BaselineItem domain = new BaselineItem();
        domain.setId(po.getId());
        domain.setBaselineCode(po.getBaselineCode());
        domain.setPartCode(po.getPartCode());
        domain.setVehicleNodeCode(po.getVehicleNodeCode());
        domain.setRemark(po.getRemark());
        return domain;
    }

    private BaselineItemPo toPo(BaselineItem domain) {
        BaselineItemPo po = new BaselineItemPo();
        po.setId(domain.getId());
        po.setBaselineCode(domain.getBaselineCode());
        po.setPartCode(domain.getPartCode());
        po.setVehicleNodeCode(domain.getVehicleNodeCode());
        po.setRemark(domain.getRemark());
        return po;
    }
}
