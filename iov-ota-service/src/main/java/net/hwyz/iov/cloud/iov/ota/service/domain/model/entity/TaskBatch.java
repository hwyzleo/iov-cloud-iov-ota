package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;
import java.math.BigDecimal;

/**
 * TaskBatch 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class TaskBatch {

    private Long id;
    private Long taskId;
    private String phase;
    private Integer batchNo;
    private BigDecimal ratio;
    private String targetExpr;
    private String state;
    private Instant releasedAt;
}
