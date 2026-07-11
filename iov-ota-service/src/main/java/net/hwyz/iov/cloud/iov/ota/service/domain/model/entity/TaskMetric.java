package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;
import java.math.BigDecimal;

/**
 * TaskMetric 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class TaskMetric {

    private Long id;
    private Long taskId;
    private Integer batchNo;
    private Integer successCnt;
    private Integer failCnt;
    private Integer timeoutCnt;
    private BigDecimal failRate;
    private BigDecimal gateThreshold;
    private String gateState;
    private Instant statTime;
}
