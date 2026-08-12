package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SequenceWatermark 连续水位测试（CR-012 §5.6）
 *
 * <p>验证事件双重唯一键和连续水位处理，乱序事件不得提前推进状态。
 *
 * @author hwyz_leo
 */
@DisplayName("SequenceWatermark 连续水位")
class SequenceWatermarkTest {

    @Test
    @DisplayName("初始水位为0，期望下一序号为1")
    void initialWatermark() {
        SequenceWatermark watermark = new SequenceWatermark();
        assertEquals(0L, watermark.getAcceptedSequenceNo());
        assertEquals(1L, watermark.expectedNext());
        assertTrue(watermark.missingRanges().isEmpty());
    }

    @Test
    @DisplayName("连续事件逐步推进水位")
    void sequentialEvents_advanceWatermark() {
        SequenceWatermark watermark = new SequenceWatermark();
        assertEquals(SequenceWatermark.Disposition.ACCEPTED, watermark.classify(1));
        watermark.tryAdvance(1);
        assertEquals(1L, watermark.getAcceptedSequenceNo());

        assertEquals(SequenceWatermark.Disposition.ACCEPTED, watermark.classify(2));
        watermark.tryAdvance(2);
        assertEquals(2L, watermark.getAcceptedSequenceNo());
    }

    @Test
    @DisplayName("小于等于水位的事件为 DUPLICATE")
    void duplicateEvent_classifiedAsDuplicate() {
        SequenceWatermark watermark = new SequenceWatermark(5L);
        assertEquals(SequenceWatermark.Disposition.DUPLICATE, watermark.classify(5));
        assertEquals(SequenceWatermark.Disposition.DUPLICATE, watermark.classify(3));
        assertEquals(5L, watermark.getAcceptedSequenceNo());
    }

    @Test
    @DisplayName("乱序事件标记 BUFFERED，不推进水位")
    void outOfOrderEvent_bufferedNotAdvance() {
        SequenceWatermark watermark = new SequenceWatermark();
        // 序号3先到，期望1
        assertEquals(SequenceWatermark.Disposition.BUFFERED, watermark.classify(3));
        watermark.buffer(3);
        assertEquals(0L, watermark.getAcceptedSequenceNo());
        assertTrue(watermark.hasBuffered());
    }

    @Test
    @DisplayName("连续事件到达后循环吸收已暂存的后续事件")
    void advance_absorbsBufferedConsecutiveEvents() {
        SequenceWatermark watermark = new SequenceWatermark();
        // 暂存 2, 3, 5
        watermark.buffer(2);
        watermark.buffer(3);
        watermark.buffer(5);
        assertEquals(0L, watermark.getAcceptedSequenceNo());

        // 1 到达，应推进到 3（吸收 2, 3），5 仍暂存
        watermark.tryAdvance(1);
        assertEquals(3L, watermark.getAcceptedSequenceNo());
        assertTrue(watermark.hasBuffered());
        assertTrue(watermark.getBufferedSequenceNos().contains(5L));

        // 4 到达，应推进到 5（吸收 4, 5）
        assertEquals(SequenceWatermark.Disposition.ACCEPTED, watermark.classify(4));
        watermark.tryAdvance(4);
        assertEquals(5L, watermark.getAcceptedSequenceNo());
        assertFalse(watermark.hasBuffered());
    }

    @Test
    @DisplayName("missingRanges 返回缺口范围")
    void missingRanges_returnsGaps() {
        SequenceWatermark watermark = new SequenceWatermark();
        // 水位0，暂存 3 和 6
        watermark.buffer(3);
        watermark.buffer(6);

        List<long[]> ranges = watermark.missingRanges();
        assertEquals(2, ranges.size());
        // 缺口1: [1, 2]
        assertArrayEquals(new long[]{1, 2}, ranges.get(0));
        // 缺口2: [4, 5]
        assertArrayEquals(new long[]{4, 5}, ranges.get(1));
    }

    @Test
    @DisplayName("非连续序号 tryAdvance 不推进")
    void tryAdvance_nonConsecutive_doesNotAdvance() {
        SequenceWatermark watermark = new SequenceWatermark();
        // 期望1，传入3，不推进
        long result = watermark.tryAdvance(3);
        assertEquals(0L, result);
        assertEquals(0L, watermark.getAcceptedSequenceNo());
    }

    @Test
    @DisplayName("重复 buffer 同一序号不重复暂存")
    void buffer_duplicateNotReBuffered() {
        SequenceWatermark watermark = new SequenceWatermark();
        watermark.buffer(3);
        watermark.buffer(3);
        assertEquals(1, watermark.getBufferedSequenceNos().size());
    }
}
