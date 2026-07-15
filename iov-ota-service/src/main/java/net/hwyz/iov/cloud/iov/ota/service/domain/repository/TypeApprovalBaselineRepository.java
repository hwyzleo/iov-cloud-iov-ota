package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TypeApprovalBaseline;

import java.util.List;
import java.util.Optional;

/**
 * TypeApprovalBaseline 仓储接口
 *
 * @author hwyz_leo
 */
public interface TypeApprovalBaselineRepository {

    Optional<TypeApprovalBaseline> getById(Long id);

    Optional<TypeApprovalBaseline> getByTaBaselineCode(String taBaselineCode);

    List<TypeApprovalBaseline> listBySwinCode(String swinCode);

    List<TypeApprovalBaseline> listByAnchor(String anchorType, String anchorCode);

    TypeApprovalBaseline save(TypeApprovalBaseline entity);

    void deleteByTaBaselineCode(String taBaselineCode);

    void deleteAll();
}
