package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.BaselineItem;

import java.util.List;
import java.util.Optional;

/**
 * BaselineItem 仓储接口
 *
 * @author hwyz_leo
 */
public interface BaselineItemRepository {

    Optional<BaselineItem> getById(Long id);

    List<BaselineItem> listByBaselineCode(String baselineCode);

    BaselineItem save(BaselineItem entity);

    void deleteById(Long id);
}
