package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import vehicle.fota.v1.Types.AvailabilityStatus;
import vehicle.fota.v1.Types.ConsentStatus;
import vehicle.fota.v1.Types.EventDisposition;
import vehicle.fota.v1.Types.ExecutionStatus;
import vehicle.fota.v1.Types.InventoryDisposition;
import vehicle.fota.v1.Types.InventoryMode;
import vehicle.fota.v1.Types.ResponseStatus;
import vehicle.fota.v1.Types.Result;
import vehicle.fota.v1.Types.VehicleTaskStatus;

import java.time.Instant;

/**
 * FOTA 协议映射与响应工具（CR-014 §5/§6.3）
 *
 * <p>领域状态字符串 ↔ vehicle.fota.v1 枚举；ResponseStatus code "0" 表示接口正常处理，
 * 不等于业务允许。
 *
 * @author hwyz_leo
 */
public final class FotaProtocols {

    private FotaProtocols() {
    }

    public static ResponseStatus ok() {
        return ResponseStatus.newBuilder()
                .setCode("0")
                .setServerTimeMs(Instant.now().toEpochMilli())
                .build();
    }

    public static ResponseStatus error(String code, String message) {
        ResponseStatus.Builder b = ResponseStatus.newBuilder()
                .setCode(code)
                .setServerTimeMs(Instant.now().toEpochMilli());
        if (message != null && !message.isBlank()) {
            b.setMessage(message);
        }
        return b.build();
    }

    // ---------------------------------------------------------------- Inventory

    public static InventoryMode inventoryMode(String mode) {
        if (mode == null) {
            return InventoryMode.INVENTORY_MODE_UNSPECIFIED;
        }
        return switch (mode) {
            case "FULL" -> InventoryMode.INVENTORY_MODE_FULL;
            case "DIGEST" -> InventoryMode.INVENTORY_MODE_DIGEST;
            default -> InventoryMode.INVENTORY_MODE_UNSPECIFIED;
        };
    }

    public static InventoryDisposition inventoryDisposition(String disposition) {
        if (disposition == null) {
            return InventoryDisposition.INVENTORY_DISPOSITION_UNSPECIFIED;
        }
        return switch (disposition) {
            case "ACCEPTED" -> InventoryDisposition.INVENTORY_DISPOSITION_ACCEPTED;
            case "FULL_REQUIRED" -> InventoryDisposition.INVENTORY_DISPOSITION_FULL_REQUIRED;
            case "REVISION_CONFLICT" -> InventoryDisposition.INVENTORY_DISPOSITION_REVISION_CONFLICT;
            case "DIGEST_MISMATCH" -> InventoryDisposition.INVENTORY_DISPOSITION_DIGEST_MISMATCH;
            case "ALGORITHM_UNSUPPORTED" -> InventoryDisposition.INVENTORY_DISPOSITION_ALGORITHM_UNSUPPORTED;
            default -> InventoryDisposition.INVENTORY_DISPOSITION_UNSPECIFIED;
        };
    }

    public static AvailabilityStatus availabilityStatus(String status) {
        if (status == null) {
            return AvailabilityStatus.AVAILABILITY_STATUS_UNSPECIFIED;
        }
        return switch (status) {
            case "NONE" -> AvailabilityStatus.AVAILABILITY_STATUS_NONE;
            case "NOT_RELEASED" -> AvailabilityStatus.AVAILABILITY_STATUS_NOT_RELEASED;
            case "AVAILABLE" -> AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE;
            case "BLOCKED" -> AvailabilityStatus.AVAILABILITY_STATUS_BLOCKED;
            case "PAUSED" -> AvailabilityStatus.AVAILABILITY_STATUS_PAUSED;
            case "CANCELED" -> AvailabilityStatus.AVAILABILITY_STATUS_CANCELED;
            case "SUPERSEDED" -> AvailabilityStatus.AVAILABILITY_STATUS_SUPERSEDED;
            default -> AvailabilityStatus.AVAILABILITY_STATUS_UNSPECIFIED;
        };
    }

    // ---------------------------------------------------------------- Consent

    public static ConsentStatus consentStatus(String action) {
        if (action == null) {
            return ConsentStatus.CONSENT_STATUS_UNSPECIFIED;
        }
        return switch (action) {
            case "GRANT" -> ConsentStatus.CONSENT_STATUS_ACCEPTED;
            case "DENY" -> ConsentStatus.CONSENT_STATUS_REJECTED;
            case "REVOKE" -> ConsentStatus.CONSENT_STATUS_REVOKED;
            default -> ConsentStatus.CONSENT_STATUS_UNSPECIFIED;
        };
    }

    // ---------------------------------------------------------------- Result

    public static Result result(String status) {
        if (status == null) {
            return Result.RESULT_UNSPECIFIED;
        }
        return switch (status) {
            case "SUCCESS", "SUCCEEDED" -> Result.RESULT_SUCCEEDED;
            case "FAILED", "FAIL" -> Result.RESULT_FAILED;
            case "PARTIAL" -> Result.RESULT_PARTIAL;
            case "ROLLED_BACK", "ROLLBACK" -> Result.RESULT_ROLLED_BACK;
            case "CANCELED", "CANCEL" -> Result.RESULT_CANCELED;
            default -> Result.RESULT_UNSPECIFIED;
        };
    }

    public static EventDisposition eventDisposition(String disposition) {
        if (disposition == null) {
            return EventDisposition.EVENT_DISPOSITION_UNSPECIFIED;
        }
        return switch (disposition) {
            case "ACCEPTED" -> EventDisposition.EVENT_DISPOSITION_ACCEPTED;
            case "DUPLICATE" -> EventDisposition.EVENT_DISPOSITION_DUPLICATE;
            case "BUFFERED" -> EventDisposition.EVENT_DISPOSITION_BUFFERED;
            case "REJECTED" -> EventDisposition.EVENT_DISPOSITION_REJECTED;
            case "CONFLICT" -> EventDisposition.EVENT_DISPOSITION_CONFLICT;
            default -> EventDisposition.EVENT_DISPOSITION_UNSPECIFIED;
        };
    }

    // ---------------------------------------------------------------- Status

    public static VehicleTaskStatus vehicleTaskStatus(String status) {
        if (status == null) {
            return VehicleTaskStatus.VEHICLE_TASK_STATUS_UNSPECIFIED;
        }
        return switch (status) {
            case "CREATED", "VISIBLE", "DISCOVERED" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_DISCOVERED;
            case "CONSENT_PENDING" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_CONSENT_PENDING;
            case "DOWNLOAD_PENDING" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_DOWNLOAD_PENDING;
            case "DOWNLOADING" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_DOWNLOADING;
            case "READY_TO_INSTALL", "READY" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_READY;
            case "WAITING_WINDOW" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_WAITING_WINDOW;
            case "PERMIT_PENDING" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_PERMIT_PENDING;
            case "EXECUTING" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_EXECUTING;
            case "PAUSED" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_PAUSED;
            case "RETRY_PENDING" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_RETRY_PENDING;
            case "ROLLBACK_PENDING" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_ROLLBACK_PENDING;
            case "SUCCEEDED", "COMPLETED" -> VehicleTaskStatus.VEHICLE_TASK_STATUS_COMPLETED;
            case "FAILED", "ROLLED_BACK", "CANCELED", "SUPERSEDED", "ENDED" ->
                    VehicleTaskStatus.VEHICLE_TASK_STATUS_ENDED;
            default -> VehicleTaskStatus.VEHICLE_TASK_STATUS_UNSPECIFIED;
        };
    }

    public static ExecutionStatus executionStatus(String status) {
        if (status == null) {
            return ExecutionStatus.EXECUTION_STATUS_UNSPECIFIED;
        }
        return switch (status) {
            case "PERMITTED" -> ExecutionStatus.EXECUTION_STATUS_PERMIT_PERSISTED;
            case "INSTALLING" -> ExecutionStatus.EXECUTION_STATUS_INSTALLING;
            case "PAUSED" -> ExecutionStatus.EXECUTION_STATUS_PAUSED;
            case "ROLLING_BACK" -> ExecutionStatus.EXECUTION_STATUS_ROLLING_BACK;
            case "SUCCEEDED" -> ExecutionStatus.EXECUTION_STATUS_SUCCEEDED;
            case "FAILED" -> ExecutionStatus.EXECUTION_STATUS_FAILED;
            case "ROLLED_BACK" -> ExecutionStatus.EXECUTION_STATUS_ROLLED_BACK;
            case "CANCELED" -> ExecutionStatus.EXECUTION_STATUS_CANCELED;
            case "TIMED_OUT" -> ExecutionStatus.EXECUTION_STATUS_FAILED;
            default -> ExecutionStatus.EXECUTION_STATUS_UNSPECIFIED;
        };
    }
}
