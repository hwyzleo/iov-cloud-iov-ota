package net.hwyz.iov.cloud.iov.ota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中央计算平台车辆升级任务状态
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskVehicleStateCcp {

    /**
     * 升级任务车辆主键
     */
    private Long id;

    /**
     * 升级任务ID
     */
    private Long taskId;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 任务状态
     */
    private Integer taskState;

}
