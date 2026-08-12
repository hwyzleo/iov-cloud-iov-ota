package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.OtaExecutionPo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * Execution 聚合与 OtaExecutionPo 转换器（CR-012）
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class ExecutionConverter {

    public Execution toDomain(OtaExecutionPo po) {
        if (po == null) {
            return null;
        }
        return Execution.reconstitute(
                ExecutionId.of(po.getId()),
                VehicleTaskId.of(po.getVehicleTaskId()),
                po.getAttemptNo(),
                ExecutionStatus.valOf(po.getStatus()),
                po.getTaskRevision() != null ? TaskRevision.of(po.getTaskRevision()) : TaskRevision.initial(),
                po.getInstallPlanVersion(),
                po.getPackageManifestDigest() != null ? SnapshotDigest.of(po.getPackageManifestDigest()) : null,
                po.getConditionSetVersion(),
                po.getPermitToken() != null ? PermitToken.of(po.getPermitToken(), toInstant(po.getValidUntil())) : null,
                toInstant(po.getValidUntil()),
                po.getAcceptedSequenceNo() != null ? po.getAcceptedSequenceNo() : 0L,
                po.getFinalSequenceNo() != null ? po.getFinalSequenceNo() : 0L,
                po.getOfflinePolicy(),
                po.getTimeoutPolicy(),
                po.getControlPolicy()
        );
    }

    public OtaExecutionPo toPo(Execution ex) {
        OtaExecutionPo po = new OtaExecutionPo();
        po.setId(ex.getId().getValue());
        po.setExecutionId(String.valueOf(ex.getId().getValue()));
        po.setVehicleTaskId(ex.getVehicleTaskId().getValue());
        po.setAttemptNo(ex.getAttemptNo());
        po.setStatus(ex.getStatus() != null ? ex.getStatus().getValue() : null);
        po.setTaskRevision(ex.getTaskRevision() != null ? ex.getTaskRevision().getValue() : null);
        po.setInstallPlanVersion(ex.getInstallPlanVersion());
        po.setPackageManifestDigest(ex.getPackageManifestDigest() != null ? ex.getPackageManifestDigest().getValue() : null);
        po.setConditionSetVersion(ex.getConditionSetVersion());
        po.setPermitToken(ex.getPermitToken() != null ? ex.getPermitToken().getToken() : null);
        po.setValidUntil(toDate(ex.getValidUntil()));
        po.setAcceptedSequenceNo(ex.getSequenceWatermark().getAcceptedSequenceNo());
        po.setFinalSequenceNo(ex.getFinalSequenceNo());
        po.setOfflinePolicy(ex.getOfflinePolicy());
        po.setTimeoutPolicy(ex.getTimeoutPolicy());
        po.setControlPolicy(ex.getControlPolicy());
        return po;
    }

    private Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    private Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }
}
