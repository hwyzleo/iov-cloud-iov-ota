package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 管理后台车辆任务重置请求（IOV-OTA-DSN-CR-025 §4.29.6）。
 *
 * <p>{@code force=true} 表示确认「执行中重置」风险，允许云端在同一事务内强制收口
 * 活动 Execution；未提供时遇到活动执行将以 403003 拒绝。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskVehicleResetMpt extends BaseRequest {

    /**
     * 重置原因（必填，≤512）
     */
    private String reason;

    /**
     * 是否强制重置（存在活动执行时必须显式 true）
     */
    private Boolean force;

    /**
     * 可选乐观锁版本（提供时校验并发覆盖）
     */
    private Long rowVersion;
}
