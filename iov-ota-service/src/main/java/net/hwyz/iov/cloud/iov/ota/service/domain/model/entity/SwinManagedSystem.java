package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * SwinManagedSystem 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class SwinManagedSystem {

    private Long id;
    private String swinCode;
    private String vehicleNodeCode;
    private Boolean isTypeApprovalRelevant;
    private String approvedSoftwareBaseline;
}
