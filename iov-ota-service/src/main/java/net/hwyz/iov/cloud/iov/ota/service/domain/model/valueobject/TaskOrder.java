package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

/**
 * 任务波次顺序值对象（IOV-OTA-DSN-CR-017）
 * <p>由 Task 创建应用服务在 Activity + Phase 作用域内解析完成，领域对象只接收已解析结果，
 * 不得在领域内部读取数据库或对缺失字段作 0／NULL 静默默认。</p>
 *
 * @param sequenceNo      活动内放量波次序（同一 activityId + phase 内从 0 连续分配，不可修改、不可复用）
 * @param previousTaskId  前序任务ID（phase 首波可空；sequence>0 时由系统推导绑定同作用域 sequence-1 的唯一 Task）
 * @author hwyz_leo
 */
public record TaskOrder(long sequenceNo, Long previousTaskId) {

    /**
     * phase 首波（sequence=0）且无前序任务
     */
    public static TaskOrder firstWave() {
        return new TaskOrder(0L, null);
    }

    /**
     * 后续波次：携带系统推导的前序任务ID
     */
    public static TaskOrder laterWave(long sequenceNo, Long previousTaskId) {
        return new TaskOrder(sequenceNo, previousTaskId);
    }
}
