package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskConsentPo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VehicleTaskConsent 领域实体与 PO 转换器（CR-016）
 *
 * @author hwyz_leo
 */
@Component
public class VehicleTaskConsentConverter {

    public VehicleTaskConsent toDomain(VehicleTaskConsentPo po) {
        if (po == null) {
            return null;
        }
        return new VehicleTaskConsent()
                .setId(po.getId())
                .setVehicleTaskId(po.getVehicleTaskId())
                .setTaskId(po.getTaskId())
                .setVin(po.getVin())
                .setTaskRevision(po.getTaskRevision())
                .setResult(ConsentResult.valOf(po.getConsentResult()))
                .setConsentReceiptId(po.getConsentReceiptId())
                .setSupersedesConsentId(po.getSupersedesConsentId())
                .setArticleId(po.getArticleId())
                .setArticleVersion(po.getArticleVersion())
                .setArticleHash(po.getArticleHash())
                .setConsentScopeDigest(po.getConsentScopeDigest())
                .setChannel(po.getChannel())
                .setSubjectRef(po.getSubjectRef())
                .setReportedAt(toInstant(po.getReportedAt()))
                .setReceivedAt(toInstant(po.getReceivedAt()))
                .setExpireAt(toInstant(po.getExpireAt()))
                .setMessageId(po.getMessageId())
                .setIdempotencyKey(po.getIdempotencyKey())
                .setRequestDigest(po.getRequestDigest())
                .setSourceModel(po.getSourceModel());
    }

    public List<VehicleTaskConsent> toDomainList(List<VehicleTaskConsentPo> poList) {
        return poList.stream().map(this::toDomain).collect(Collectors.toList());
    }

    public VehicleTaskConsentPo toPo(VehicleTaskConsent entity) {
        VehicleTaskConsentPo po = new VehicleTaskConsentPo();
        po.setId(entity.getId());
        po.setVehicleTaskId(entity.getVehicleTaskId());
        po.setTaskId(entity.getTaskId());
        po.setVin(entity.getVin());
        po.setTaskRevision(entity.getTaskRevision());
        po.setConsentResult(entity.getResult() != null ? entity.getResult().getValue() : null);
        po.setConsentReceiptId(entity.getConsentReceiptId());
        po.setSupersedesConsentId(entity.getSupersedesConsentId());
        po.setArticleId(entity.getArticleId());
        po.setArticleVersion(entity.getArticleVersion());
        po.setArticleHash(entity.getArticleHash());
        po.setConsentScopeDigest(entity.getConsentScopeDigest());
        po.setChannel(entity.getChannel());
        po.setSubjectRef(entity.getSubjectRef());
        po.setReportedAt(toDate(entity.getReportedAt()));
        po.setReceivedAt(toDate(entity.getReceivedAt()));
        po.setExpireAt(toDate(entity.getExpireAt()));
        po.setMessageId(entity.getMessageId());
        po.setIdempotencyKey(entity.getIdempotencyKey());
        po.setRequestDigest(entity.getRequestDigest());
        po.setSourceModel(entity.getSourceModel());
        return po;
    }

    private Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    private Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }
}
