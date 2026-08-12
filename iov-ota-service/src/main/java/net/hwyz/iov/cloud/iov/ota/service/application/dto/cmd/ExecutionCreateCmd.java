package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 安装执行创建命令（CR-012 §5.5、US-079）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCreateCmd {

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 幂等键 */
    private String idempotencyKey;

    /** 安装计划版本 */
    private String installPlanVersion;

    /** 包清单摘要 */
    private String packageManifestDigest;

    /** 期望的包清单摘要（重新计算校验） */
    private String expectedPackageManifestDigest;

    /** 条件集版本 */
    private String conditionSetVersion;

    /** 离线策略（JSON） */
    private String offlinePolicy;

    /** 超时策略（JSON） */
    private String timeoutPolicy;

    /** 控制策略（JSON） */
    private String controlPolicy;

    /** 车架号 */
    private String vin;
}
