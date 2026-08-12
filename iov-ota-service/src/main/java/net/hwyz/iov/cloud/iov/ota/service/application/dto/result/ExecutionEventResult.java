package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 安装事件结果（CR-012 §5.6、US-080）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEventResult {

    /** 事件处置：ACCEPTED/DUPLICATE/BUFFERED/REJECTED/CONFLICT */
    private String disposition;

    /** 当前连续水位 */
    private long acceptedSequenceNo;

    /** 缺失序号范围列表，每项 [start, end] */
    private List<long[]> missingSequenceRanges;

    /** 最新有效控制的 revision（事件响应可携带） */
    private Integer latestControlRevision;

    /** 最新有效控制的动作 */
    private String latestControlAction;
}
