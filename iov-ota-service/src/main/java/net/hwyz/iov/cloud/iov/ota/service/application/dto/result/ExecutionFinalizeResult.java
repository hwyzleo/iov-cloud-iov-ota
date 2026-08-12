package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 安装执行收口结果（CR-012 §5.7、US-081）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionFinalizeResult {

    /** 结果是否已接受 */
    private boolean resultAccepted;

    /** 缺失序号范围（未接受时返回） */
    private List<long[]> missingSequenceRanges;

    /** 执行最终状态 */
    private String executionStatus;

    /** 车辆任务最终状态 */
    private String vehicleTaskStatus;
}
