package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 车辆升级任务完整过程视图（CR-015 §3.3）
 * <p>聚合权威数据（tb_task_vehicle / inventory / consent / package / execution / control / ecu_result / upgrade_log / delivery_observation）。
 * 默认不返回 raw Envelope、payload bytes、下载凭证或完整 VIN。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskVehicleProcessResult {

    /** 车辆任务ID */
    private Long taskVehicleId;

    /** 脱敏 VIN（默认不返回完整 VIN） */
    private String vinMasked;

    /** 车辆任务/任务/活动基础信息 */
    private TaskVehicleProcessView vehicleTask;

    /** 已接受 ECU 清单摘要 */
    private InventoryProcessSummary inventorySummary;

    /** 授权摘要 */
    private ConsentProcessSummary consentSummary;

    /** 包下载/验签/解密阶段 */
    private List<PackageStageProcessView> packageStages;

    /** 安装尝试（Execution）列表 */
    private List<ExecutionProcessView> executions;

    /** 云端控制与回执摘要 */
    private ControlProcessSummary controls;

    /** Execution 收口后的 ECU 实际版本与结果 */
    private List<EcuResultProcessView> ecuResultSummary;

    /** 升级日志上传摘要 */
    private List<UpgradeLogProcessView> logSummary;

    /** VAGW 技术投递观测（独立技术维度，不计入业务成功） */
    private List<DeliveryObservationProcessView> deliveryObservationSummary;
}
