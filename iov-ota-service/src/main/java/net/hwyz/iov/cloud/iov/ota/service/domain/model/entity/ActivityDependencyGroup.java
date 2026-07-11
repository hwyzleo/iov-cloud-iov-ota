package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * ActivityDependencyGroup 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class ActivityDependencyGroup {

    private Long id;
    private Long activityId;
    private String groupCode;
    private String memberNodeCode;
    private Boolean rollbackTogether;
}
