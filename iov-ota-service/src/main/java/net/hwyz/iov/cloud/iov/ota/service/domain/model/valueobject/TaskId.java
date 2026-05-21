package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;
import java.util.Objects;

@Getter
public class TaskId {
    
    private final Long value;
    
    public static TaskId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Task ID must be a positive number");
        }
        return new TaskId(value);
    }
    
    private TaskId(Long value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskId taskId = (TaskId) o;
        return Objects.equals(value, taskId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return "TaskId{" + "value=" + value + '}';
    }
}