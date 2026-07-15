package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TypeApprovalBaseline;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TypeApprovalBaselineItem;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TypeApprovalBaselineRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TypeApprovalBaselineItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TypeApprovalBaselineMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TypeApprovalBaselineItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TypeApprovalBaselinePo;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TypeApprovalBaseline Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TypeApprovalBaselineRepositoryImpl implements TypeApprovalBaselineRepository {

    private final TypeApprovalBaselineMapper mapper;
    private final TypeApprovalBaselineItemMapper itemMapper;

    @Override
    public Optional<TypeApprovalBaseline> getById(Long id) {
        TypeApprovalBaselinePo po = mapper.selectById(id);
        if (po == null || !Boolean.TRUE.equals(po.getRowValid())) {
            return Optional.empty();
        }
        TypeApprovalBaseline domain = toDomain(po);
        domain.setItems(listItemsByTaBaselineCode(po.getTaBaselineCode()));
        return Optional.of(domain);
    }

    @Override
    public Optional<TypeApprovalBaseline> getByTaBaselineCode(String taBaselineCode) {
        LambdaQueryWrapper<TypeApprovalBaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TypeApprovalBaselinePo::getTaBaselineCode, taBaselineCode);
        TypeApprovalBaselinePo po = mapper.selectOne(wrapper);
        if (po == null || !Boolean.TRUE.equals(po.getRowValid())) {
            return Optional.empty();
        }
        TypeApprovalBaseline domain = toDomain(po);
        domain.setItems(listItemsByTaBaselineCode(taBaselineCode));
        return Optional.of(domain);
    }

    @Override
    public List<TypeApprovalBaseline> listBySwinCode(String swinCode) {
        LambdaQueryWrapper<TypeApprovalBaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TypeApprovalBaselinePo::getSwinCode, swinCode)
               .eq(TypeApprovalBaselinePo::getRowValid, true);
        List<TypeApprovalBaselinePo> poList = mapper.selectList(wrapper);
        List<TypeApprovalBaseline> result = new ArrayList<>();
        for (TypeApprovalBaselinePo po : poList) {
            TypeApprovalBaseline domain = toDomain(po);
            domain.setItems(listItemsByTaBaselineCode(po.getTaBaselineCode()));
            result.add(domain);
        }
        return result;
    }

    @Override
    public List<TypeApprovalBaseline> listByAnchor(String anchorType, String anchorCode) {
        LambdaQueryWrapper<TypeApprovalBaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TypeApprovalBaselinePo::getAnchorType, anchorType)
               .eq(TypeApprovalBaselinePo::getAnchorCode, anchorCode)
               .eq(TypeApprovalBaselinePo::getRowValid, true);
        List<TypeApprovalBaselinePo> poList = mapper.selectList(wrapper);
        List<TypeApprovalBaseline> result = new ArrayList<>();
        for (TypeApprovalBaselinePo po : poList) {
            TypeApprovalBaseline domain = toDomain(po);
            domain.setItems(listItemsByTaBaselineCode(po.getTaBaselineCode()));
            result.add(domain);
        }
        return result;
    }

    @Override
    public TypeApprovalBaseline save(TypeApprovalBaseline entity) {
        TypeApprovalBaselinePo po = toPo(entity);
        if (entity.getId() == null) {
            mapper.insert(po);
            entity.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        // 保存明细：先删后插
        saveItems(entity.getTaBaselineCode(), entity.getItems());
        return entity;
    }

    @Override
    public void deleteByTaBaselineCode(String taBaselineCode) {
        // 删除明细
        LambdaQueryWrapper<TypeApprovalBaselineItemPo> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(TypeApprovalBaselineItemPo::getTaBaselineCode, taBaselineCode);
        itemMapper.delete(itemWrapper);

        // 删除主表
        LambdaQueryWrapper<TypeApprovalBaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TypeApprovalBaselinePo::getTaBaselineCode, taBaselineCode);
        mapper.delete(wrapper);
        log.info("已删除TA基线投影: taBaselineCode={}", taBaselineCode);
    }

    @Override
    public void deleteAll() {
        itemMapper.delete(null);
        mapper.delete(null);
        log.info("已删除所有TA基线投影");
    }

    private List<TypeApprovalBaselineItem> listItemsByTaBaselineCode(String taBaselineCode) {
        LambdaQueryWrapper<TypeApprovalBaselineItemPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TypeApprovalBaselineItemPo::getTaBaselineCode, taBaselineCode);
        List<TypeApprovalBaselineItemPo> poList = itemMapper.selectList(wrapper);
        List<TypeApprovalBaselineItem> result = new ArrayList<>();
        for (TypeApprovalBaselineItemPo po : poList) {
            result.add(toItemDomain(po));
        }
        return result;
    }

    private void saveItems(String taBaselineCode, List<TypeApprovalBaselineItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (TypeApprovalBaselineItem item : items) {
            item.setTaBaselineCode(taBaselineCode);
            TypeApprovalBaselineItemPo itemPo = toItemPo(item);
            if (item.getId() == null) {
                itemMapper.insert(itemPo);
                item.setId(itemPo.getId());
            } else {
                itemMapper.updateById(itemPo);
            }
        }
    }

    private TypeApprovalBaseline toDomain(TypeApprovalBaselinePo po) {
        TypeApprovalBaseline domain = new TypeApprovalBaseline();
        domain.setId(po.getId());
        domain.setTaBaselineCode(po.getTaBaselineCode());
        domain.setSwinCode(po.getSwinCode());
        domain.setAnchorType(po.getAnchorType());
        domain.setAnchorCode(po.getAnchorCode());
        domain.setStatus(po.getStatus());
        domain.setProjectionDigest(po.getProjectionDigest());
        domain.setEffectiveFrom(po.getEffectiveFrom() != null ? po.getEffectiveFrom().toInstant() : null);
        domain.setSourceBaselineScope(po.getSourceBaselineScope());
        domain.setUpVersion(po.getUpVersion());
        return domain;
    }

    private TypeApprovalBaselinePo toPo(TypeApprovalBaseline domain) {
        TypeApprovalBaselinePo po = new TypeApprovalBaselinePo();
        po.setId(domain.getId());
        po.setTaBaselineCode(domain.getTaBaselineCode());
        po.setSwinCode(domain.getSwinCode());
        po.setAnchorType(domain.getAnchorType());
        po.setAnchorCode(domain.getAnchorCode());
        po.setStatus(domain.getStatus());
        po.setProjectionDigest(domain.getProjectionDigest());
        po.setEffectiveFrom(domain.getEffectiveFrom() != null ? java.util.Date.from(domain.getEffectiveFrom()) : null);
        po.setSourceBaselineScope(domain.getSourceBaselineScope());
        po.setUpVersion(domain.getUpVersion());
        return po;
    }

    private TypeApprovalBaselineItem toItemDomain(TypeApprovalBaselineItemPo po) {
        TypeApprovalBaselineItem domain = new TypeApprovalBaselineItem();
        domain.setId(po.getId());
        domain.setTaBaselineCode(po.getTaBaselineCode());
        domain.setVehicleNodeCode(po.getVehicleNodeCode());
        domain.setPartCode(po.getPartCode());
        domain.setApprovedVersion(po.getApprovedVersion());
        domain.setSourceBaselineCode(po.getSourceBaselineCode());
        return domain;
    }

    private TypeApprovalBaselineItemPo toItemPo(TypeApprovalBaselineItem domain) {
        TypeApprovalBaselineItemPo po = new TypeApprovalBaselineItemPo();
        po.setId(domain.getId());
        po.setTaBaselineCode(domain.getTaBaselineCode());
        po.setVehicleNodeCode(domain.getVehicleNodeCode());
        po.setPartCode(domain.getPartCode());
        po.setApprovedVersion(domain.getApprovedVersion());
        po.setSourceBaselineCode(domain.getSourceBaselineCode());
        return po;
    }
}
