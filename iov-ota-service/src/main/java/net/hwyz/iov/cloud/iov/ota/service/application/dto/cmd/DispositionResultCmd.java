package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 本地任务/缓存处置结果命令（CR-012 §5.1、US-076）
 *
 * <p>车端可因活动执行、回滚依赖、checkpoint 或证据保留而延期/拒绝处置。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositionResultCmd {

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 任务版本号 */
    private Long taskRevision;

    /** 处置：ACCEPT / DEFER / REJECT */
    private String disposition;

    /** 处置原因 */
    private String reason;

    /** 包缓存动作：KEEP / RELEASE */
    private String packageCacheAction;

    /** 车架号 */
    private String vin;
}
