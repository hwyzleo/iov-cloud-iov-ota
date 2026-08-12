package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;

import java.util.Objects;

/**
 * 任务版本号值对象（CR-012 §2.2、§5.9）
 *
 * <p>taskRevision 是 VehicleTask 的单调递增版本号。已冻结任务的实质性变化必须升级 taskRevision；
 * 非实质性 revision 变化不自动使 receipt 失效。
 *
 * @author hwyz_leo
 */
@Getter
public class TaskRevision implements Comparable<TaskRevision> {

    private final long value;

    public static TaskRevision of(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("TaskRevision must not be negative");
        }
        return new TaskRevision(value);
    }

    public static TaskRevision initial() {
        return new TaskRevision(1L);
    }

    private TaskRevision(long value) {
        this.value = value;
    }

    /**
     * 升级版本号，返回新的 TaskRevision 实例（不可变）。
     */
    public TaskRevision next() {
        return new TaskRevision(this.value + 1);
    }

    /**
     * 判断目标版本是否为本版本的实质性升级。
     */
    public boolean isUpgradeFrom(TaskRevision other) {
        return other == null || this.value > other.value;
    }

    @Override
    public int compareTo(TaskRevision o) {
        return Long.compare(this.value, o.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskRevision that = (TaskRevision) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "TaskRevision{value=" + value + '}';
    }
}
