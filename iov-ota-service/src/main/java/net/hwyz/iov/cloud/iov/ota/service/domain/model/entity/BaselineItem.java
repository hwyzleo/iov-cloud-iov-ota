package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * BaselineItem 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class BaselineItem {

    private Long id;
    private String baselineCode;
    private String partCode;
    private String vehicleNodeCode;
    private String remark;
}
