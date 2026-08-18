package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskConsentCurrentResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleTaskStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ConsentPolicy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 单 VehicleTask 授权查询服务（CR-016 §6、US-102～105）
 *
 * <p>返回不可变授权历史与当前权威状态（含有效/无效原因）。管理后台无授权写接口。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTaskConsentQueryService {

    private final VehicleTaskRepository vehicleTaskRepository;
    private final VehicleTaskConsentRepository vehicleTaskConsentRepository;
    private final ConsentPolicy consentPolicy;

    /**
     * 单 VehicleTask 的不可变授权历史（按接收时间升序）。
     */
    public List<VehicleTaskConsentResult> listHistory(Long vehicleTaskId) {
        return vehicleTaskConsentRepository.findByVehicleTaskId(vehicleTaskId)
                .stream()
                .map(this::toResult)
                .collect(Collectors.toList());
    }

    /**
     * 当前权威状态、receipt 与有效/无效原因。
     */
    public VehicleTaskConsentCurrentResult getCurrent(Long vehicleTaskId) {
        VehicleTask vt = vehicleTaskRepository.getById(VehicleTaskId.of(vehicleTaskId))
                .orElseThrow(() -> new VehicleTaskStateException("车辆任务[" + vehicleTaskId + "]不存在"));
        VehicleTaskConsent current = vehicleTaskConsentRepository
                .findCurrentByVehicleTaskId(vehicleTaskId).orElse(null);
        Instant now = Instant.now();
        String invalidReason = consentPolicy.invalidReason(vt, current, now);

        return VehicleTaskConsentCurrentResult.builder()
                .vehicleTaskId(vehicleTaskId)
                .consentState(vt.getConsentState() != null ? vt.getConsentState().getValue() : null)
                .currentConsentId(vt.getCurrentConsentId())
                .currentReceiptId(current != null ? current.getConsentReceiptId() : null)
                .scopeDigest(vt.getConsentScopeDigest())
                .articleVersion(vt.getConsentArticleVersion())
                .consentUpdatedAt(vt.getConsentUpdatedAt())
                .valid(invalidReason == null)
                .invalidReason(invalidReason)
                .build();
    }

    private VehicleTaskConsentResult toResult(VehicleTaskConsent c) {
        return VehicleTaskConsentResult.builder()
                .consentRecordId(c.getId())
                .consentResult(c.getResult() != null ? c.getResult().getValue() : null)
                .consentReceiptId(c.getConsentReceiptId())
                .taskRevision(c.getTaskRevision())
                .articleId(c.getArticleId())
                .articleVersion(c.getArticleVersion())
                .articleHash(c.getArticleHash())
                .scopeDigest(c.getConsentScopeDigest())
                .channel(c.getChannel())
                .subjectRef(c.getSubjectRef())
                .reportedAt(c.getReportedAt())
                .receivedAt(c.getReceivedAt())
                .expireAt(c.getExpireAt())
                .supersedesConsentId(c.getSupersedesConsentId())
                .sourceModel(c.getSourceModel())
                .build();
    }
}
