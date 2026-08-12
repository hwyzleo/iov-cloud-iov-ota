package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 控制回执命令（CR-012 §5.6、US-080）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlAckCmd {

    /** 执行ID */
    private Long executionId;

    /** 控制回执ID（幂等键） */
    private String controlAckId;

    /** 控制ID */
    private String controlId;

    /** 回执序号 */
    private Integer ackSequenceNo;

    /** 回执状态：RECEIVED/DEFERRED/APPLIED/REJECTED */
    private String ackStatus;

    /** 回执负载（JSON） */
    private String ackPayload;

    /** 车架号 */
    private String vin;
}
