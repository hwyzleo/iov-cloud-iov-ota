package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;

/**
 * UpgradeLog 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class UpgradeLog {

    private Long id;
    private Long taskId;
    private String vin;
    private String logUrl;
    private String uploadState;
    private Instant uploadTime;
}
