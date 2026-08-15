package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.EcuResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionFinalizeCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionFinalizeResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Execution.EcuResult;
import vehicle.fota.v1.Execution.FinalResultReport;
import vehicle.fota.v1.Execution.FinalResultResponse;
import vehicle.fota.v1.Types.Result;
import vehicle.fota.v1.Types.SequenceRange;

import java.util.List;

/**
 * 最终结果收口命令处理器（CR-014 §5：vehicle.fota.v1.FinalResultReport）
 *
 * <p>水位未达最终序号时 result_accepted=false 并携带缺失范围（US-081）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class FinalResultCommandHandler {

    private final ExecutionAppService executionAppService;

    public FinalResultResponse handle(FotaMessageMetadata md, FinalResultReport req) {
        ExecutionFinalizeCmd cmd = new ExecutionFinalizeCmd();
        cmd.setVin(md.vin());
        cmd.setExecutionId(parseLong(md.executionId()));
        cmd.setFinalStatus(mapFinalStatus(req.getResult()));
        cmd.setFinalSequenceNo(req.getFinalSequenceNo());
        cmd.setResultDigest(req.hasResultDigest() ? req.getResultDigest().getValueHex() : null);
        if (req.getEcuResultsCount() > 0) {
            cmd.setEcuResults(req.getEcuResultsList().stream()
                    .map(FinalResultCommandHandler::toEcuResultCmd).toList());
        }

        ExecutionFinalizeResult result = executionAppService.finalizeExecution(cmd);

        FinalResultResponse.Builder b = FinalResultResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setResultAccepted(result.isResultAccepted())
                .setExecutionStatus(FotaProtocols.executionStatus(result.getExecutionStatus()))
                .setAcceptedSequenceNo(0L)
                .setVehicleTaskStatus(FotaProtocols.vehicleTaskStatus(result.getVehicleTaskStatus()))
                .setNeedLogs(false)
                .setNextAction(result.isResultAccepted() ? "DONE" : "RESEND");
        if (result.getMissingSequenceRanges() != null) {
            for (long[] r : result.getMissingSequenceRanges()) {
                b.addMissingSequenceRanges(SequenceRange.newBuilder()
                        .setStart(r[0]).setEnd(r[1]).build());
            }
        }
        return b.build();
    }

    private static EcuResultCmd toEcuResultCmd(EcuResult ecu) {
        return EcuResultCmd.builder()
                .ecuId(ecu.getEcuId())
                .targetSoftwareVersion(ecu.getTargetVersion())
                .actualSoftwareVersion(ecu.hasActualVersion() ? ecu.getActualVersion() : null)
                .result(ecu.getResult() == Result.RESULT_SUCCEEDED ? "SUCCESS"
                        : ecu.getResult() == Result.RESULT_ROLLED_BACK ? "ROLLED_BACK" : "FAILED")
                .failReason(ecu.hasFailureStage() ? ecu.getFailureStage() : null)
                .build();
    }

    private static String mapFinalStatus(Result result) {
        if (result == null) {
            return null;
        }
        return switch (result) {
            case RESULT_SUCCEEDED -> "SUCCEEDED";
            case RESULT_ROLLED_BACK -> "ROLLED_BACK";
            case RESULT_PARTIAL -> "PARTIAL";
            default -> "FAILED";
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
