package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityGroupPolicy;

import java.util.List;
import java.util.Optional;

/**
 * ActivityGroupPolicy 仓储接口
 *
 * @author hwyz_leo
 * @since 2026-07-13
 */
public interface ActivityGroupPolicyRepository {

    Optional<ActivityGroupPolicy> getById(Long id);

    List<ActivityGroupPolicy> listByActivityId(Long activityId);

    Optional<ActivityGroupPolicy> getByActivityIdAndGroupNo(Long activityId, Integer groupNo);

    ActivityGroupPolicy save(ActivityGroupPolicy entity);

    void deleteById(Long id);

    void deleteByActivityId(Long activityId);
}