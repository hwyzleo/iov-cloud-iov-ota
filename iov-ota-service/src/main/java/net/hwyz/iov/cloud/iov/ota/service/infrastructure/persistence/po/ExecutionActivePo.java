package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * 活动执行占位表 PO（CR-012 §3、RD-012-5）
 *
 * <p>UK(vehicle_task_id) 保证同一 VehicleTask 同时最多一个活动 Execution。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_execution_active")
public class ExecutionActivePo {

    private static final long serialVersionUID = 1L;

    @TableId("vehicle_task_id")
    private Long vehicleTaskId;

    private Long executionId;

    private Date createTime;
}
