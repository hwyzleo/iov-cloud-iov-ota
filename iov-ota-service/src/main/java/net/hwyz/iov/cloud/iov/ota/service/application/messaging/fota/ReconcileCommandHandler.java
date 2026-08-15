package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.RecoveryQueryCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.RecoveryResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.RecoveryAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Reconcile.ExecutionDisposition;
import vehicle.fota.v1.Reconcile.QueryScope;
import vehicle.fota.v1.Reconcile.ReconcileRequest;
import vehicle.fota.v1.Reconcile.ReconcileResponse;
import vehicle.fota.v1.Types.SequenceRange;

/**
 * 状态对账命令处理器（CR-014 §5：vehicle.fota.v1.ReconcileRequest）
 *
 * <p>VehicleTask/Execution 恢复对账（US-083）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class ReconcileCommandHandler {

    private final RecoveryAppService recoveryAppService;

    public ReconcileResponse handle(FotaMessageMetadata md, ReconcileRequest req) {
        RecoveryQueryCmd cmd = new RecoveryQueryCmd();
        cmd.setVin(md.vin());
        cmd.setScope(req.getQueryScope() == QueryScope.QUERY_SCOPE_EXECUTION ? "EXECUTION" : "VEHICLE_TASK");
        cmd.setVehicleTaskId(parseLong(md.vehicleTaskId()));
        cmd.setExecutionId(parseLong(md.executionId()));

        RecoveryResult result = recoveryAppService.query(cmd);

        ReconcileResponse.Builder b = ReconcileResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setVehicleTaskStatus(FotaProtocols.vehicleTaskStatus(result.getVehicleTaskStatus()))
                .setExecutionDisposition(executionDisposition(result.getDisposition()))
                .setExecutionStatus(FotaProtocols.executionStatus(result.getExecutionStatus()))
                .setNextAction(result.getRecoveryAction() == null ? "DONE" : result.getRecoveryAction());
        if (result.getAcceptedSequenceNo() != null) {
            b.setAcceptedSequenceNo(result.getAcceptedSequenceNo());
        }
        if (result.getMissingSequenceRanges() != null) {
            for (long[] r : result.getMissingSequenceRanges()) {
                b.addMissingSequenceRanges(SequenceRange.newBuilder()
                        .setStart(r[0]).setEnd(r[1]).build());
            }
        }
        return b.build();
    }

    private static ExecutionDisposition executionDisposition(String disposition) {
        if (disposition == null) {
            return ExecutionDisposition.EXECUTION_DISPOSITION_UNSPECIFIED;
        }
        return switch (disposition) {
            case "CONSISTENT" -> ExecutionDisposition.EXECUTION_DISPOSITION_CONSISTENT;
            case "CLOUD_ONLY" -> ExecutionDisposition.EXECUTION_DISPOSITION_CLOUD_ONLY;
            case "VEHICLE_ONLY" -> ExecutionDisposition.EXECUTION_DISPOSITION_VEHICLE_ONLY;
            case "REVISION_CONFLICT" -> ExecutionDisposition.EXECUTION_DISPOSITION_REVISION_CONFLICT;
            case "MANUAL_RECOVERY_REQUIRED" -> ExecutionDisposition.EXECUTION_DISPOSITION_MANUAL_RECOVERY_REQUIRED;
            default -> ExecutionDisposition.EXECUTION_DISPOSITION_UNSPECIFIED;
        };
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
