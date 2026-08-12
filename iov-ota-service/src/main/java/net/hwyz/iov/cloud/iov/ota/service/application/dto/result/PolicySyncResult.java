package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略同步结果（CR-012 §5.9、US-084）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicySyncResult {

    /** 有效策略（JSON） */
    private String effectivePolicy;

    /** 用户偏好版本号 */
    private Long preferenceVersion;

    /** 是否有策略变化需升级 taskRevision */
    private boolean revisionUpgradeRequired;
}
