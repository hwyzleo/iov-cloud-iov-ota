package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

/**
 * 车辆任务已创建事件（CR-012 §2.2）
 *
 * @author hwyz_leo
 */
@Getter
public class VehicleTaskCreatedEvent extends VehicleTaskEvent {

    private final Long taskId;
    private final String vin;
    private final long taskRevision;

    public VehicleTaskCreatedEvent(VehicleTaskId vehicleTaskId, Long taskId, String vin, long taskRevision) {
        super(vehicleTaskId);
        this.taskId = taskId;
        this.vin = vin;
        this.taskRevision = taskRevision;
    }
}
