package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 顺序事件连续水位值对象（CR-012 §5.6）
 *
 * <p>事件以双重唯一键（eventId + executionId/sequenceNo）处理。acceptedSequenceNo 只推进到
 * 连续接收水位；乱序事件可 BUFFER，但只有连续事件才可推进 Execution 状态。
 *
 * <p>不变量：
 * <ul>
 *   <li>acceptedSequenceNo 单调不减</li>
 *   <li>sequenceNo == acceptedSequenceNo + 1 时推进水位，并循环吸收已暂存的后续连续事件</li>
 *   <li>小于等于水位且摘要一致的事件为 DUPLICATE</li>
 *   <li>大于下一期望序号的事件标记 BUFFERED，不得提前推进业务状态</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Getter
public class SequenceWatermark {

    /** 当前已接受的连续序号水位，0 表示尚未接收任何事件 */
    private long acceptedSequenceNo;
    /** 已暂存的乱序事件序号（大于下一期望序号） */
    private final List<Long> bufferedSequenceNos;

    public SequenceWatermark() {
        this(0L);
    }

    public SequenceWatermark(long acceptedSequenceNo) {
        this(acceptedSequenceNo, new ArrayList<>());
    }

    public SequenceWatermark(long acceptedSequenceNo, List<Long> bufferedSequenceNos) {
        this.acceptedSequenceNo = acceptedSequenceNo;
        this.bufferedSequenceNos = new ArrayList<>(bufferedSequenceNos);
    }

    /**
     * 期望的下一连续序号。
     */
    public long expectedNext() {
        return acceptedSequenceNo + 1;
    }

    /**
     * 判定给定序号的事件应被如何处置（不改变状态）。
     *
     * @param sequenceNo 待判定序号
     * @return ACCEPTED / DUPLICATE / BUFFERED
     */
    public Disposition classify(long sequenceNo) {
        if (sequenceNo <= acceptedSequenceNo) {
            return Disposition.DUPLICATE;
        }
        if (sequenceNo == expectedNext()) {
            return Disposition.ACCEPTED;
        }
        return Disposition.BUFFERED;
    }

    /**
     * 尝试推进水位。当 sequenceNo == expectedNext() 时推进，并循环吸收已暂存的后续连续事件。
     *
     * @param sequenceNo 被接受的事件序号
     * @return 本次推进后新达到的水位；若序号非连续则不推进，返回原水位
     */
    public long tryAdvance(long sequenceNo) {
        if (sequenceNo != expectedNext()) {
            return acceptedSequenceNo;
        }
        acceptedSequenceNo = sequenceNo;
        // 循环吸收已暂存的后续连续事件
        while (bufferedSequenceNos.remove(Long.valueOf(acceptedSequenceNo + 1))) {
            acceptedSequenceNo++;
        }
        return acceptedSequenceNo;
    }

    /**
     * 重置水位（仅用于从持久化重建）。
     */
    public void reset(long acceptedSequenceNo) {
        this.acceptedSequenceNo = acceptedSequenceNo;
        this.bufferedSequenceNos.clear();
    }

    /**
     * 暂存一个乱序事件序号。
     */
    public void buffer(long sequenceNo) {
        if (sequenceNo > acceptedSequenceNo && !bufferedSequenceNos.contains(sequenceNo)) {
            bufferedSequenceNos.add(sequenceNo);
            Collections.sort(bufferedSequenceNos);
        }
    }

    /**
     * 返回当前缺失的序号范围（水位与最大暂存序号之间的缺口）。
     *
     * @return 缺失范围列表，每项为 [start, end] 闭区间；无缺口时返回空列表
     */
    public List<long[]> missingRanges() {
        List<long[]> ranges = new ArrayList<>();
        long cursor = acceptedSequenceNo + 1;
        for (Long buffered : bufferedSequenceNos) {
            if (buffered > cursor) {
                ranges.add(new long[]{cursor, buffered - 1});
            }
            cursor = buffered + 1;
        }
        return ranges;
    }

    /**
     * 是否还有未吸收的暂存事件。
     */
    public boolean hasBuffered() {
        return !bufferedSequenceNos.isEmpty();
    }

    public List<Long> getBufferedSequenceNos() {
        return Collections.unmodifiableList(bufferedSequenceNos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequenceWatermark that = (SequenceWatermark) o;
        return acceptedSequenceNo == that.acceptedSequenceNo
                && Objects.equals(bufferedSequenceNos, that.bufferedSequenceNos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(acceptedSequenceNo, bufferedSequenceNos);
    }

    @Override
    public String toString() {
        return "SequenceWatermark{accepted=" + acceptedSequenceNo
                + ", buffered=" + bufferedSequenceNos + '}';
    }

    /** 序号分类结果（内部使用，与 api 层 EventDisposition 语义对应） */
    public enum Disposition {
        ACCEPTED, DUPLICATE, BUFFERED
    }
}
