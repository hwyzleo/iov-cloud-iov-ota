package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 管理后台升级任务暂停请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskPauseMpt extends BaseRequest {

    /**
     * 暂停原因：MANUAL/GATE_HALT/RISK_HALT/COMPLIANCE_HALT
     */
    private String pauseReason;

    /**
     * 发起方：HUMAN/SYSTEM
     */
    private String pausedBy;

}