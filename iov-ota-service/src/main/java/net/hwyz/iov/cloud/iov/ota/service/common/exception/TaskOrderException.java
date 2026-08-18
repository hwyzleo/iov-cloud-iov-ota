package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 任务波次顺序与前序关系异常（IOV-OTA-DSN-CR-017）
 * <p>Task 创建应用服务在 Activity + Phase 作用域内并发排号、推导并持久化 previousTaskId 时，
 * 遇到序号冲突、前序缺失／歧义、作用域不符等情况 fail-closed 阻断。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
public class TaskOrderException extends OtaBaseException {

    /** 显式序号与服务端下一可分配序号不一致或并发冲突 */
    public static final int ERROR_CODE_SEQUENCE_CONFLICT = 411101;
    /** 显式或自动前序任务不存在 */
    public static final int ERROR_CODE_PREVIOUS_NOT_FOUND = 411102;
    /** 历史重复序号产生多个前序候选 */
    public static final int ERROR_CODE_PREVIOUS_AMBIGUOUS = 411103;
    /** 前序属于其他 Activity 或违反 phase／顺序规则 */
    public static final int ERROR_CODE_PREVIOUS_SCOPE_MISMATCH = 411104;
    /** 后续波次发布时前序关系缺失（release 侧守卫，默认走 TaskReleaseGateException） */
    public static final int ERROR_CODE_PREVIOUS_RELATION_MISSING = 411105;

    public TaskOrderException(int errorCode, String message) {
        super(errorCode, message);
        log.warn("任务顺序异常[{}]: {}", errorCode, message);
    }

    public static TaskOrderException sequenceConflict(long activityId, Integer expected, Integer provided) {
        return new TaskOrderException(ERROR_CODE_SEQUENCE_CONFLICT,
                "任务序号冲突：活动[" + activityId + "]当前可分配序号[" + expected + "]，请求序号[" + provided + "]不匹配");
    }

    public static TaskOrderException previousNotFound(Long previousTaskId) {
        return new TaskOrderException(ERROR_CODE_PREVIOUS_NOT_FOUND,
                "前序任务[" + previousTaskId + "]不存在");
    }

    public static TaskOrderException previousMissing(Long activityId, String phase, long sequenceNo) {
        return new TaskOrderException(ERROR_CODE_PREVIOUS_NOT_FOUND,
                "前序推导失败：活动[" + activityId + "]阶段[" + phase + "]序号[" + sequenceNo + "]无前序任务，fail-closed 拒绝创建");
    }

    public static TaskOrderException previousAmbiguous(Long activityId, String phase, long sequenceNo, int size) {
        return new TaskOrderException(ERROR_CODE_PREVIOUS_AMBIGUOUS,
                "前序任务歧义：活动[" + activityId + "]阶段[" + phase + "]序号[" + sequenceNo + "]存在[" + size + "]个候选");
    }

    public static TaskOrderException previousScopeMismatch(String message) {
        return new TaskOrderException(ERROR_CODE_PREVIOUS_SCOPE_MISMATCH, message);
    }
}
