package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;

/**
 * UpgradePackageBuild 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class UpgradePackageBuild {

    private Long id;
    private Long upgradePackageId;
    private String buildType;
    private String state;
    private String errorMsg;
    private Instant startTime;
    private Instant endTime;
}
