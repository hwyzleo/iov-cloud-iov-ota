package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略同步命令（CR-012 §5.9、US-084）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicySyncCmd {

    /** 车架号 */
    private String vin;

    /** 用户偏好基础版本号（乐观并发） */
    private Long basePreferenceVersion;

    /** 用户偏好（JSON） */
    private String userPreference;

    /** 车辆任务ID（可选，用于冻结策略校验） */
    private Long vehicleTaskId;
}
