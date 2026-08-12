package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 安装事件命令（CR-012 §5.6、US-080）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEventCmd {

    /** 执行ID */
    private Long executionId;

    /** 事件ID（幂等键） */
    private String eventId;

    /** 事件序号 */
    private Long sequenceNo;

    /** 事件类型 */
    private String eventType;

    /** 事件摘要（防冲突） */
    private String eventDigest;

    /** 事件负载（JSON） */
    private String eventPayload;

    /** 车架号 */
    private String vin;
}
