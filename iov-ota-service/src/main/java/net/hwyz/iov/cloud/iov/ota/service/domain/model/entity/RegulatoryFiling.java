package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * RegulatoryFiling 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class RegulatoryFiling {

    private Long id;
    private Long activityId;
    private String filingType;
    private String swContentRef;
    private String releaseNoteRef;
    private String filingStatus;
    private String filingNo;
}
