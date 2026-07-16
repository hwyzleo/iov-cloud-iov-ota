package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 升级任务状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum TaskState {

    // 新四段式状态（US-062）
    DRAFT("草稿", 1),
    PENDING_APPROVAL("待审批", 2),
    APPROVED("已审批", 3),
    REJECTED("已驳回", 4),
    SCHEDULED("已排程", 5),
    RELEASED("已发布", 6),
    PAUSED("已暂停", 7),
    COMPLETED("已完成", 8),
    CANCELED("已取消", 9),
    SUPERSEDED("已取代", 10),
    IN_PROGRESS("执行中", 11);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static TaskState valOf(Integer val) {
        if (val == null) {
            return null;
        }
        // 新状态值映射（US-062/066）
        switch (val) {
            case 1: return DRAFT;
            case 2: return PENDING_APPROVAL;
            case 3: return APPROVED;
            case 4: return REJECTED;
            case 5: return SCHEDULED;
            case 6: return RELEASED;
            case 7: return PAUSED;
            case 8: return COMPLETED;
            case 9: return CANCELED;
            case 10: return SUPERSEDED;
            case 11: return IN_PROGRESS;
            default:
                // 兼容旧状态值映射（迁移后旧值已被转换，此分支理论上不会执行）
                switch (val) {
                    case 1: return DRAFT;          // 旧 PENDING -> DRAFT
                    case 2: return PENDING_APPROVAL; // 旧 SUBMITTED -> PENDING_APPROVAL
                    case 3: return APPROVED;       // 旧 APPROVED -> APPROVED
                    case 4: return REJECTED;       // 旧 REJECTED -> REJECTED
                    case 5: return RELEASED;       // 旧 RELEASED -> RELEASED (值6)
                    case 6: return PAUSED;         // 旧 PAUSED -> PAUSED (值7)
                    case 7: return COMPLETED;      // 旧 FINISHED -> COMPLETED (值8)
                    case 8: return CANCELED;       // 旧 CANCELLED -> CANCELED (值9)
                    default:
                        return null;
                }
        }
    }

}
