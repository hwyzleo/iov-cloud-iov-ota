package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 管理后台升级任务审核（支持多级审批）
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
     * 审批级别：QUALITY/PRODUCT/SECURITY
     */
    private String approvalLevel;

    /**
     * 审批结果：APPROVED/REJECTED
     */
    private String result;

    /**
     * 审批意见
     */
    private String comment;

}
