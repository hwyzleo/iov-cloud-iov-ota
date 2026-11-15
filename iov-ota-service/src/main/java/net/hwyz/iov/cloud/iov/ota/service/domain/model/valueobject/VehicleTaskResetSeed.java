package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;

import java.time.Instant;

/**
 * 车辆任务重置落库种子（IOV-OTA-DSN-CR-025 §4.29.3）。
 *
 * <p>承载「重置时新建初始化车辆任务」所需的全部不可变取值：
 * 沿用发布期冻结的发布时间/执行窗口/包清单摘要与授权要求，
 * 状态与下载准备按发布建档口径初始化，授权不继承旧车辆任务。</p>
 *
 * @param activityId            升级活动ID
 * @param taskId                升级任务ID
 * @param vin                   车架号
 * @param sourceVehicleTaskId   原车辆任务ID（写入 description 供溯源与快照内容继承）
 * @param status                新车辆任务初始状态（DOWNLOAD_PENDING / CONSENT_PENDING）
 * @param consentRequired       是否需授权（取父任务冻结值）
 * @param consentState          初始授权状态（PENDING / NOT_REQUIRED）
 * @param consentArticleId      冻结条款身份引用
 * @param consentArticleVersion 冻结条款展示版本
 * @param consentArticleHash    冻结条款权威摘要
 * @param releaseAt             发布时间快照（复用父任务冻结值）
 * @param startTime             执行窗口开始快照
 * @param endTime               执行窗口结束快照
 * @param taskRevision          新车辆任务修订号（初始值 1）
 * @param snapshotDigest        包清单摘要（复用父任务冻结值，不重算）
 * @param operator              操作人（写入 create_by）
 * @author hwyz_leo
 */
public record VehicleTaskResetSeed(
        Long activityId,
        Long taskId,
        String vin,
        Long sourceVehicleTaskId,
        VehicleTaskStatus status,
        boolean consentRequired,
        ConsentState consentState,
        Long consentArticleId,
        String consentArticleVersion,
        String consentArticleHash,
        Instant releaseAt,
        Instant startTime,
        Instant endTime,
        Long taskRevision,
        String snapshotDigest,
        String operator) {
}
