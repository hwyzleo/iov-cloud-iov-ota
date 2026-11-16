package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradeLog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * UpgradeLog 仓储接口
 *
 * @author hwyz_leo
 */
public interface UpgradeLogRepository {

    Optional<UpgradeLog> getById(Long id);

    /**
     * 按调用方幂等键查询（EDD-OSS requestId，CR-026）。
     */
    Optional<UpgradeLog> findByLogRequestId(String logRequestId);

    /**
     * 按对象键查询（对象键是事实锚点，CR-026）。
     */
    Optional<UpgradeLog> findByObjectKey(String objectKey);

    /**
     * 按车辆任务分页查询（SQL 过滤，CR-026 §8.2）。
     */
    List<UpgradeLog> listByVehicleTask(Long vehicleTaskId, Instant beginTime, Instant endTime, String uploadState);

    UpgradeLog save(UpgradeLog entity);

    void deleteById(Long id);
}
