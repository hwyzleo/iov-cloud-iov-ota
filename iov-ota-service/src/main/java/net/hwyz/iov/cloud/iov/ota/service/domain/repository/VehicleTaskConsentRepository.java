package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * VehicleTask 授权事实仓储接口（CR-016 §3.2/§3.3）
 *
 * <p>授权历史追加写入；当前状态由 VehicleTask 聚合（tb_task_vehicle）维护，
 * 当前授权记录通过 current_consent_id 解析。
 *
 * @author hwyz_leo
 */
public interface VehicleTaskConsentRepository {

    /**
     * 追加授权历史并返回带主键的记录。
     */
    VehicleTaskConsent append(VehicleTaskConsent consent);

    Optional<VehicleTaskConsent> findById(Long id);

    Optional<VehicleTaskConsent> findByMessageId(String messageId);

    Optional<VehicleTaskConsent> findByIdempotencyKey(String idempotencyKey);

    Optional<VehicleTaskConsent> findByReceiptId(String consentReceiptId);

    /**
     * 解析 tb_task_vehicle.current_consent_id 指向的当前权威授权记录。
     */
    Optional<VehicleTaskConsent> findCurrentByVehicleTaskId(Long vehicleTaskId);

    /**
     * 单个 VehicleTask 的不可变授权历史（按接收时间升序）。
     */
    List<VehicleTaskConsent> findByVehicleTaskId(Long vehicleTaskId);

    /**
     * Task 级授权历史查询（可带结果与时间过滤）。
     */
    List<VehicleTaskConsent> findByTaskId(Long taskId, String consentResult,
                                          Instant beginTime, Instant endTime);
}
