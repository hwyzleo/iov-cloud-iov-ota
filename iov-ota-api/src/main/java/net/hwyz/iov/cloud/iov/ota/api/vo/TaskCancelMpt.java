package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 管理后台升级任务取消请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskCancelMpt extends BaseRequest {

    /**
     * 取消原因：DISCARD/ABORT/ROLLBACK/COMPLIANCE/SUPERSEDED_BY
     */
    private String cancelReason;

}