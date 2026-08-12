package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 云端控制命令（CR-013 §4：下行 ota.execution.control.issued）
 *
 * <p>云端控制是带作用域和安全应用模式的意图，车端保留本地安全裁决权（RD-012-6）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlCommandCmd {

    /** 执行ID */
    private Long executionId;

    /** 控制动作：CONTINUE/PAUSE/ABORT/ROLLBACK/RESYNC */
    private String action;

    /** 控制作用域 */
    private String scope;

    /** 应用模式：IMMEDIATE/SAFE_POINT */
    private String applyMode;

    /** 控制原因 */
    private String reason;

    /** 车架号 */
    private String vin;
}
