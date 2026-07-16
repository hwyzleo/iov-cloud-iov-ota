package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 重试/续传轨迹审计领域实体
 * 对应表：tb_task_vehicle_retry_log
 */
@Getter
@Setter
@Builder
public class TaskVehicleRetryLog {
    
    private Long id;
    private Long taskId;
    private String vin;
    private String stage;  // 阶段：DOWNLOAD/INSTALL
    private Integer attemptNo;  // 尝试次数
    private Long offset;  // 偏移量（字节）
    private String result;  // 结果：SUCCESS/FAIL
    private String reason;  // 原因
    private Instant retriedAt;  // 重试时间
    private String description;  // 备注
    
    /**
     * 创建下载重试日志
     *
     * @param taskId 任务ID
     * @param vin VIN
     * @param attemptNo 尝试次数
     * @param offset 偏移量
     * @param result 结果
     * @param reason 原因
     * @return 重试日志
     */
    public static TaskVehicleRetryLog createDownloadLog(Long taskId, String vin, Integer attemptNo, 
                                                         Long offset, String result, String reason) {
        return TaskVehicleRetryLog.builder()
                .taskId(taskId)
                .vin(vin)
                .stage("DOWNLOAD")
                .attemptNo(attemptNo)
                .offset(offset)
                .result(result)
                .reason(reason)
                .retriedAt(Instant.now())
                .build();
    }
    
    /**
     * 创建安装重试日志
     *
     * @param taskId 任务ID
     * @param vin VIN
     * @param attemptNo 尝试次数
     * @param result 结果
     * @param reason 原因
     * @return 重试日志
     */
    public static TaskVehicleRetryLog createInstallLog(Long taskId, String vin, Integer attemptNo, 
                                                        String result, String reason) {
        return TaskVehicleRetryLog.builder()
                .taskId(taskId)
                .vin(vin)
                .stage("INSTALL")
                .attemptNo(attemptNo)
                .result(result)
                .reason(reason)
                .retriedAt(Instant.now())
                .build();
    }
    
    /**
     * 判断是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(result);
    }
    
    /**
     * 判断是否失败
     *
     * @return 是否失败
     */
    public boolean isFail() {
        return "FAIL".equals(result);
    }
    
    /**
     * 判断是否为下载阶段
     *
     * @return 是否为下载阶段
     */
    public boolean isDownloadStage() {
        return "DOWNLOAD".equals(stage);
    }
    
    /**
     * 判断是否为安装阶段
     *
     * @return 是否为安装阶段
     */
    public boolean isInstallStage() {
        return "INSTALL".equals(stage);
    }
}
