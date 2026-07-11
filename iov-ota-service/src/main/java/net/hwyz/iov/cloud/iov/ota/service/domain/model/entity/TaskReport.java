package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;
import java.math.BigDecimal;

/**
 * TaskReport 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class TaskReport {

    private Long id;
    private Long taskId;
    private BigDecimal completeRate;
    private BigDecimal successRate;
    private String failCaseDist;
    private Instant genTime;
}
