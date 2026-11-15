package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆任务重置异常（IOV-OTA-DSN-CR-025 §4.29.6）。
 *
 * <p>错误码段：403000～403999（IOV-OTA 项目段，见 IOV-OTA-错误码）。
 * message 内保留稳定错误标识，便于管理端按标识映射提示文案。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleTaskResetException extends OtaBaseException {

    /** 403001：非当前车辆任务（已被取代或无当前行）。 */
    public static final int ERROR_CODE_NOT_CURRENT = 403001;
    /** 403002：状态为 SUCCEEDED，禁止重置。 */
    public static final int ERROR_CODE_SUCCEEDED_NOT_RESETTABLE = 403002;
    /** 403003：存在活动 Execution 但未指定 force=true。 */
    public static final int ERROR_CODE_NEED_FORCE = 403003;
    /** 403004：父任务状态不允许重置（非 RELEASED/IN_PROGRESS）。 */
    public static final int ERROR_CODE_TASK_STATE_BLOCKED = 403004;
    /** 403005：已过父任务 endTime，禁止重置。 */
    public static final int ERROR_CODE_WINDOW_EXPIRED = 403005;
    /** 403006：重置原因缺失或超长。 */
    public static final int ERROR_CODE_REASON_REQUIRED = 403006;
    /** 403007：并发/重复重置，或 rowVersion 冲突。 */
    public static final int ERROR_CODE_CONFLICT = 403007;

    /** 稳定错误标识（与 IOV-OTA-错误码 文档一致）。 */
    public static final String IDENT_NOT_CURRENT = "OTA-VEHICLE-TASK-NOT-CURRENT";
    public static final String IDENT_SUCCEEDED_NOT_RESETTABLE = "OTA-VEHICLE-TASK-SUCCEEDED-NOT-RESETTABLE";
    public static final String IDENT_NEED_FORCE = "OTA-VEHICLE-TASK-RESET-NEED-FORCE";
    public static final String IDENT_TASK_STATE_BLOCKED = "OTA-VEHICLE-TASK-RESET-TASK-STATE-BLOCKED";
    public static final String IDENT_WINDOW_EXPIRED = "OTA-VEHICLE-TASK-RESET-WINDOW-EXPIRED";
    public static final String IDENT_REASON_REQUIRED = "OTA-VEHICLE-TASK-RESET-REASON-REQUIRED";
    public static final String IDENT_CONFLICT = "OTA-VEHICLE-TASK-RESET-CONFLICT";

    public VehicleTaskResetException(int errorCode, String ident, String message) {
        super(errorCode, "[" + ident + "] " + message);
        log.warn("车辆任务重置异常[{}][{}]: {}", errorCode, ident, message);
    }

    public static VehicleTaskResetException notCurrent(Long vehicleTaskId) {
        return new VehicleTaskResetException(ERROR_CODE_NOT_CURRENT, IDENT_NOT_CURRENT,
                "车辆任务[" + vehicleTaskId + "]不是当前车辆任务，不可重置");
    }

    public static VehicleTaskResetException succeededNotResettable(Long vehicleTaskId) {
        return new VehicleTaskResetException(ERROR_CODE_SUCCEEDED_NOT_RESETTABLE, IDENT_SUCCEEDED_NOT_RESETTABLE,
                "车辆任务[" + vehicleTaskId + "]已成功，不支持重置");
    }

    public static VehicleTaskResetException needForce(Long vehicleTaskId, Long executionId) {
        return new VehicleTaskResetException(ERROR_CODE_NEED_FORCE, IDENT_NEED_FORCE,
                "车辆任务[" + vehicleTaskId + "]存在活动执行[" + executionId + "]，需显式确认后强制重置");
    }

    public static VehicleTaskResetException taskStateBlocked(Long taskId, String state) {
        return new VehicleTaskResetException(ERROR_CODE_TASK_STATE_BLOCKED, IDENT_TASK_STATE_BLOCKED,
                "任务[" + taskId + "]当前状态[" + state + "]不允许重置车辆任务");
    }

    public static VehicleTaskResetException windowExpired(Long taskId) {
        return new VehicleTaskResetException(ERROR_CODE_WINDOW_EXPIRED, IDENT_WINDOW_EXPIRED,
                "任务[" + taskId + "]已过执行窗口结束时间，不可重置车辆任务");
    }

    public static VehicleTaskResetException reasonRequired() {
        return new VehicleTaskResetException(ERROR_CODE_REASON_REQUIRED, IDENT_REASON_REQUIRED,
                "重置原因必填且长度不超过 512");
    }

    public static VehicleTaskResetException conflict(Long vehicleTaskId) {
        return new VehicleTaskResetException(ERROR_CODE_CONFLICT, IDENT_CONFLICT,
                "车辆任务[" + vehicleTaskId + "]被并发或重复重置，请刷新后重试");
    }
}
