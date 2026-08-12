package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;

import java.util.Objects;

/**
 * 安装执行标识值对象（CR-012 §2.3）
 *
 * @author hwyz_leo
 */
@Getter
public class ExecutionId {

    private final Long value;

    public static ExecutionId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Execution ID must be a positive number");
        }
        return new ExecutionId(value);
    }

    private ExecutionId(Long value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionId that = (ExecutionId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ExecutionId{value=" + value + '}';
    }
}
