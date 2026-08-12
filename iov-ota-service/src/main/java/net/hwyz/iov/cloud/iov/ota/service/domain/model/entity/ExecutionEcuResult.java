package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * 执行 ECU 结果实体（CR-012 §3、§5.7）
 *
 * <p>Execution 收口后的 ECU 实际结果和版本。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExecutionEcuResult {

    private final Long id;
    private final Long executionId;
    /** ECU 标识 */
    private final String ecuId;
    /** 目标软件版本 */
    private final String targetSoftwareVersion;
    /** 实际软件版本 */
    private final String actualSoftwareVersion;
    /** 结果：SUCCESS / FAILED / ROLLED_BACK */
    private final String result;
    /** 失败原因 */
    private final String failReason;
}
