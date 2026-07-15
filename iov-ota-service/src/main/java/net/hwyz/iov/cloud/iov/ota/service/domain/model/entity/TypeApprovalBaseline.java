package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;
import java.util.List;

/**
 * TypeApprovalBaseline 领域实体
 * 型式批准基线投影（MDM EEAD TypeApprovalBaseline）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class TypeApprovalBaseline {

    private Long id;
    private String taBaselineCode;
    private String swinCode;
    private String anchorType;
    private String anchorCode;
    private String status;
    private String projectionDigest;
    private Instant effectiveFrom;
    private String sourceBaselineScope;
    private Long upVersion;
    private List<TypeApprovalBaselineItem> items;
}
