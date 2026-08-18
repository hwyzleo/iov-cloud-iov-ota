package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

/**
 * VehicleTask 聚合与 TaskVehiclePo 转换器（CR-012）
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VehicleTaskConverter {

    public VehicleTask toDomain(TaskVehiclePo po) {
        if (po == null) {
            return null;
        }
        return VehicleTask.reconstitute(
                VehicleTaskId.of(po.getId()),
                po.getTaskId(),
                po.getVin(),
                po.getTaskRevision() != null ? TaskRevision.of(po.getTaskRevision()) : TaskRevision.initial(),
                po.getSnapshotDigest() != null ? SnapshotDigest.of(po.getSnapshotDigest()) : null,
                VehicleTaskStatus.valOf(po.getVehicleTaskStatus()),
                DownloadReadyState.valOf(po.getDownloadReadyState()),
                ConsentState.valOf(po.getConsentState()),
                toInstant(po.getReleaseAt()),
                toInstant(po.getVtStartTime()),
                toInstant(po.getVtEndTime()),
                po.getSupersededBy() != null ? VehicleTaskId.of(po.getSupersededBy()) : null,
                po.getLocalDisposition(),
                po.getPackageCacheAction(),
                po.getActiveExecutionId() != null ? ExecutionId.of(po.getActiveExecutionId()) : null,
                po.getLastAttemptNo() != null ? po.getLastAttemptNo() : 0,
                VehicleTaskStatus.valOf(po.getVtStateBeforePause()),
                po.getConsentRequired() != null && po.getConsentRequired() == 1,
                po.getConsentArticleId(),
                po.getConsentArticleVersion(),
                po.getConsentArticleHash(),
                po.getConsentScopeDigest(),
                po.getCurrentConsentId(),
                toInstant(po.getConsentUpdatedAt()),
                po.getRowVersion() != null ? po.getRowVersion().longValue() : 0L
        );
    }

    public TaskVehiclePo toPo(VehicleTask vt) {
        TaskVehiclePo po = new TaskVehiclePo();
        po.setId(vt.getId().getValue());
        po.setTaskId(vt.getTaskId());
        po.setVin(vt.getVin());
        po.setVehicleTaskStatus(vt.getStatus() != null ? vt.getStatus().getValue() : null);
        po.setTaskRevision(vt.getTaskRevision() != null ? vt.getTaskRevision().getValue() : null);
        po.setSnapshotDigest(vt.getSnapshotDigest() != null ? vt.getSnapshotDigest().getValue() : null);
        po.setDownloadReadyState(vt.getDownloadReadyState() != null ? vt.getDownloadReadyState().getValue() : null);
        po.setConsentState(vt.getConsentState() != null ? vt.getConsentState().getValue() : null);
        po.setReleaseAt(toDate(vt.getReleaseAt()));
        po.setVtStartTime(toDate(vt.getStartTime()));
        po.setVtEndTime(toDate(vt.getEndTime()));
        po.setSupersededBy(vt.getSupersededBy() != null ? vt.getSupersededBy().getValue() : null);
        po.setLocalDisposition(vt.getLocalDisposition());
        po.setPackageCacheAction(vt.getPackageCacheAction());
        po.setActiveExecutionId(vt.getActiveExecutionId() != null ? vt.getActiveExecutionId().getValue() : null);
        po.setLastAttemptNo(vt.getLastAttemptNo());
        po.setVtStateBeforePause(vt.getStateBeforePause() != null ? vt.getStateBeforePause().getValue() : null);
        po.setConsentRequired(vt.isConsentRequired() ? 1 : 0);
        po.setConsentArticleId(vt.getConsentArticleId());
        po.setConsentArticleVersion(vt.getConsentArticleVersion());
        po.setConsentArticleHash(vt.getConsentArticleHash());
        po.setConsentScopeDigest(vt.getConsentScopeDigest());
        po.setCurrentConsentId(vt.getCurrentConsentId());
        po.setConsentUpdatedAt(toDate(vt.getConsentUpdatedAt()));
        po.setRowVersion(Math.toIntExact(vt.getRowVersion()));
        return po;
    }

    private Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    private Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }
}
