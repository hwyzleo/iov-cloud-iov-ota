package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 统一授权有效性判定策略（CR-016 §3.3/§4）
 *
 * <p>下载/安装许可应用服务必须调用本策略，不得由 Controller、消费者或 SQL
 * 各自实现不同规则。有效凭据必须同时满足：
 * <pre>
 * vehicleTask.consentState == GRANTED
 * AND currentConsent.vehicleTaskId == vehicleTask.id
 * AND currentConsent.taskRevision == vehicleTask.taskRevision
 * AND currentConsent.consentScopeDigest == vehicleTask.consentScopeDigest
 * AND currentConsent.articleHash == frozenTerms.articleHash
 * AND (expireAt IS NULL OR expireAt > now)
 * </pre>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class ConsentPolicy {

    /**
     * 有效授权判定。
     *
     * @param vehicleTask 车辆任务（含当前授权状态与冻结条款）
     * @param currentConsent 当前权威授权记录（tb_vehicle_task_consent）
     * @param now            当前时间
     * @return true 表示可签发下载/安装许可
     */
    public boolean isPermitted(VehicleTask vehicleTask, VehicleTaskConsent currentConsent, Instant now) {
        return invalidReason(vehicleTask, currentConsent, now) == null;
    }

    /**
     * 返回失效原因；为 null 表示凭据有效。
     */
    public String invalidReason(VehicleTask vehicleTask, VehicleTaskConsent currentConsent, Instant now) {
        if (vehicleTask == null) {
            return "车辆任务不存在";
        }
        if (!vehicleTask.isConsentRequired()) {
            return null;
        }
        if (vehicleTask.getConsentState() != net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState.GRANTED) {
            return "当前授权状态[" + vehicleTask.getConsentState() + "]非已授权";
        }
        if (currentConsent == null) {
            return "缺少当前权威授权记录";
        }
        if (!currentConsent.getVehicleTaskId().equals(vehicleTask.getId().getValue())) {
            return "授权记录与车辆任务不匹配";
        }
        if (!currentConsent.getTaskRevision().equals(vehicleTask.getTaskRevision().getValue())) {
            return "授权记录任务修订[" + currentConsent.getTaskRevision()
                    + "]与当前修订[" + vehicleTask.getTaskRevision().getValue() + "]不一致";
        }
        if (vehicleTask.getConsentScopeDigest() != null
                && !vehicleTask.getConsentScopeDigest().equals(currentConsent.getConsentScopeDigest())) {
            return "授权范围摘要不一致";
        }
        if (vehicleTask.getConsentArticleHash() != null
                && !vehicleTask.getConsentArticleHash().equals(currentConsent.getArticleHash())) {
            return "授权条款摘要与冻结条款不一致";
        }
        if (currentConsent.getExpireAt() != null && currentConsent.getExpireAt().isBefore(now)) {
            return "授权已过期";
        }
        return null;
    }
}
