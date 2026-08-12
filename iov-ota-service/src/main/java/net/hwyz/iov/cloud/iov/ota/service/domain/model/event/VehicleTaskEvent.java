package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

import java.time.Instant;

/**
 * 车辆任务领域事件基类（CR-012 §2.2）
 *
 * @author hwyz_leo
 */
@Getter
public abstract class VehicleTaskEvent {

    private final Instant occurredOn;
    private final VehicleTaskId vehicleTaskId;

    protected VehicleTaskEvent(VehicleTaskId vehicleTaskId) {
        this.vehicleTaskId = vehicleTaskId;
        this.occurredOn = Instant.now();
    }
}
