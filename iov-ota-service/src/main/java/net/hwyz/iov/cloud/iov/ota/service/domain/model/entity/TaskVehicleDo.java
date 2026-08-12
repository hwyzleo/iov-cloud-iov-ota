package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.Instant;

/**
 * 升级任务车辆领域对象
 *
 * <p>CR-013 移除 v1（CCP REST check）时代的加载/适配/旧状态转换逻辑；
 * 车辆任务运行状态由 VehicleTask 聚合（tb_task_vehicle.vehicle_task_status）承载。
 * 本实体仅保留任务车辆基础处置与重试信息，供管理侧查询使用。
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@SuperBuilder
public class TaskVehicleDo extends BaseDo<Long> implements DomainObj<TaskVehicleDo> {

    /**
     * 目标来源：CONDITION/LIST/IMPORT
     */
    private String source;

    /**
     * 准入状态：PASS/REJECT
     */
    private String admitState;

    /**
     * 准入原因（REJECT时）
     */
    private String admitReason;

    /**
     * 基线快照
     */
    private String baseline;

    /**
     * 下载重试次数
     */
    private Integer downloadRetryCount;

    /**
     * 安装重试次数
     */
    private Integer installRetryCount;

    /**
     * 续传偏移量（字节）
     */
    private Long resumeOffset;

    /**
     * 续传令牌
     */
    private String resumeToken;

    /**
     * 最近失败原因
     */
    private String lastFailReason;

    /**
     * 下次重试时间
     */
    private Instant nextRetryAt;

    /**
     * 尝试次数（幂等）
     */
    private Integer attemptNo;

    /**
     * 初始化
     */
    public void init() {
        stateInit();
    }

    /**
     * 设置准入状态
     *
     * @param admitState  准入状态
     * @param admitReason 准入原因
     */
    public void setAdmitState(String admitState, String admitReason) {
        this.admitState = admitState;
        this.admitReason = admitReason;
        stateChange();
    }

    /**
     * 记录下载失败
     *
     * @param reason  失败原因
     * @param maxRetry 最大重试次数
     */
    public void recordDownloadFailure(String reason, Integer maxRetry) {
        this.lastFailReason = reason;
        this.downloadRetryCount = (this.downloadRetryCount == null ? 0 : this.downloadRetryCount) + 1;
        this.attemptNo = (this.attemptNo == null ? 0 : this.attemptNo) + 1;
        stateChange();
    }

    /**
     * 记录安装失败
     *
     * @param reason  失败原因
     * @param maxRetry 最大重试次数
     */
    public void recordInstallFailure(String reason, Integer maxRetry) {
        this.lastFailReason = reason;
        this.installRetryCount = (this.installRetryCount == null ? 0 : this.installRetryCount) + 1;
        this.attemptNo = (this.attemptNo == null ? 0 : this.attemptNo) + 1;
        stateChange();
    }

    /**
     * 设置续传信息
     *
     * @param offset 续传偏移量
     * @param token  续传令牌
     */
    public void setResumeInfo(Long offset, String token) {
        this.resumeOffset = offset;
        this.resumeToken = token;
        stateChange();
    }

    /**
     * 清除续传信息
     */
    public void clearResumeInfo() {
        this.resumeOffset = null;
        this.resumeToken = null;
        stateChange();
    }

    /**
     * 是否可以重试下载
     *
     * @param maxRetry 最大重试次数
     * @return 是否可以重试
     */
    public boolean canRetryDownload(Integer maxRetry) {
        if (maxRetry == null || maxRetry <= 0) {
            return false;
        }
        int currentRetry = this.downloadRetryCount == null ? 0 : this.downloadRetryCount;
        return currentRetry < maxRetry;
    }

    /**
     * 是否可以重试安装
     *
     * @param maxRetry 最大重试次数
     * @return 是否可以重试
     */
    public boolean canRetryInstall(Integer maxRetry) {
        if (maxRetry == null || maxRetry <= 0) {
            return false;
        }
        int currentRetry = this.installRetryCount == null ? 0 : this.installRetryCount;
        return currentRetry < maxRetry;
    }

    /**
     * 是否有续传信息
     *
     * @return 是否有续传信息
     */
    public boolean hasResumeInfo() {
        return this.resumeOffset != null && this.resumeToken != null;
    }
}
