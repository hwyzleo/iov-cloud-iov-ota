package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 升级活动发布异常
 * 活动未处于可发布状态（已审核）或型批相关且评估状态未通过时抛出
 *
 * @author hwyz_leo
 */
@Slf4j
public class ActivityReleaseException extends OtaBaseException {

    private static final int ERROR_CODE = 411005;

    public ActivityReleaseException(Long activityId, String reason) {
        super(ERROR_CODE, "升级活动[" + activityId + "]发布失败：" + reason);
        log.warn("升级活动[{}]发布失败：{}", activityId, reason);
    }

}
