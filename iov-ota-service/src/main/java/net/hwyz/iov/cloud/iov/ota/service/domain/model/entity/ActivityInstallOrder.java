package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * ActivityInstallOrder 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class ActivityInstallOrder {

    private Long id;
    private Long activityId;
    private String vehicleNodeCode;
    private Integer seqNo;
    private Integer parallelGroup;
}
