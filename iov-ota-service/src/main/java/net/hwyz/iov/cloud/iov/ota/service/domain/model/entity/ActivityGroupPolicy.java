package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * ActivityGroupPolicy 领域实体
 * 活动分组策略
 *
 * @author hwyz_leo
 * @since 2026-07-13
 */
@Getter
@Setter
@Accessors(chain = true)
public class ActivityGroupPolicy {

    private Long id;
    private Long activityId;
    private Integer groupNo;
    private Boolean rollbackTogether;
    private Boolean atomicActivation;
    private Boolean unifiedReboot;
    private Integer failurePolicy;
    private Integer failThreshold;
}