package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;
import java.util.Objects;

@Getter
public class ActivityId {
    
    private final Long value;
    
    public static ActivityId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Activity ID must be a positive number");
        }
        return new ActivityId(value);
    }
    
    private ActivityId(Long value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityId that = (ActivityId) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}