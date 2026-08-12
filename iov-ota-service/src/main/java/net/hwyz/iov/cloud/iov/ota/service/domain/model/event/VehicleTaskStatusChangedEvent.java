package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

/**
 * 车辆任务状态变更事件（CR-012 §2.2）
 *
 * @author hwyz_leo
 */
@Getter
public class VehicleTaskStatusChangedEvent extends VehicleTaskEvent {

    private final VehicleTaskStatus previousStatus;
    private final VehicleTaskStatus currentStatus;

    public VehicleTaskStatusChangedEvent(VehicleTaskId vehicleTaskId, VehicleTaskStatus previousStatus, VehicleTaskStatus currentStatus) {
        super(vehicleTaskId);
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
    }
}
