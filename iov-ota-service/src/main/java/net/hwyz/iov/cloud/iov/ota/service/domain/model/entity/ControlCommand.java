package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ControlAction;

/**
 * 云端控制指令实体（CR-012 §3、§5.6）
 *
 * <p>云端控制是带作用域和安全应用模式的意图，车端保留本地安全裁决权。
 * 控制指令按 controlRevision 单调递增；UK(control_id)、UK(execution_id, control_revision)。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ControlCommand {

    private final Long id;
    /** 控制ID（幂等键） */
    private final String controlId;
    private final Long executionId;
    /** 控制版本号，单调递增 */
    private final int controlRevision;
    /** 控制动作 */
    private final ControlAction action;
    /** 控制作用域 */
    private final String scope;
    /** 应用模式：IMMEDIATE / SAFE_POINT */
    private final String applyMode;
    /** 控制原因 */
    private final String reason;
}
