package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 恢复查询命令（CR-012 §5.8、US-083）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryQueryCmd {

    /** 查询范围：VEHICLE_TASK / EXECUTION */
    private String scope;

    /** 车辆任务ID（VEHICLE_TASK 范围） */
    private Long vehicleTaskId;

    /** 执行ID（EXECUTION 范围） */
    private Long executionId;

    /** 车架号 */
    private String vin;
}
