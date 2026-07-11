package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * ActivityTargetVersion 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class ActivityTargetVersion {

    private Long id;
    private Long activityId;
    private String baselineCode;
    private String vehicleNodeCode;
    private String partCode;
    private String targetSoftwareBuildVer;
}
