package net.hwyz.iov.cloud.iov.ota.api.contract;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.web.domain.BaseRequest;

/**
 * 管理后台升级任务审核
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskAuditMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 审核是否通过：true=通过，false=未通过
     */
    private Boolean audit;

    /**
     * 未通过的原因
     */
    private String reason;

}
