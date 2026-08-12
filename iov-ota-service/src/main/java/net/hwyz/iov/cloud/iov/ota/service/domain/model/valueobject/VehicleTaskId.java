package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;

import java.util.Objects;

/**
 * 车辆任务标识值对象（CR-012 §2.2）
 *
 * @author hwyz_leo
 */
@Getter
public class VehicleTaskId {

    private final Long value;

    public static VehicleTaskId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("VehicleTask ID must be a positive number");
        }
        return new VehicleTaskId(value);
    }

    private VehicleTaskId(Long value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleTaskId that = (VehicleTaskId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "VehicleTaskId{value=" + value + '}';
    }
}
