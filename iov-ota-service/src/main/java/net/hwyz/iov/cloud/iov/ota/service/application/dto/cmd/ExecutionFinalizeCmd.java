package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 安装执行收口命令（CR-012 §5.7、US-081）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionFinalizeCmd {

    /** 执行ID */
    private Long executionId;

    /** 最终状态：SUCCEEDED/FAILED/ROLLED_BACK */
    private String finalStatus;

    /** 最终序号 */
    private Long finalSequenceNo;

    /** 结果摘要（幂等校验） */
    private String resultDigest;

    /** ECU 结果列表 */
    private List<EcuResultCmd> ecuResults;

    /** 车架号 */
    private String vin;
}
