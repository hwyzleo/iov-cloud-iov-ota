package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * TypeApprovalBaselineItem 领域实体
 * 型式批准基线明细
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class TypeApprovalBaselineItem {

    private Long id;
    private String taBaselineCode;
    private String vehicleNodeCode;
    private String partCode;
    private String approvedVersion;
    private String sourceBaselineCode;
}
