package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;

/**
 * ApprovedSwManifest 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class ApprovedSwManifest {

    private Long id;
    private String manifestCode;
    private Long activityId;
    private String swinCode;
    private String rxswinValue;
    private String manifestStatus;
    private Instant approveTime;
}
