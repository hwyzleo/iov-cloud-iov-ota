package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ECU 软件单元结果命令（CR-019 §4.23.5）
 *
 * <p>与 Proto SoftwareUnitResult 对齐；SINGLE_IMAGE 写一条 Target=ECU_IMAGE。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareUnitResultCmd {

    /** 软件目标编码 */
    private String softwareTargetCode;
    /** 源版本 */
    private String sourceVersion;
    /** 目标版本 */
    private String targetVersion;
    /** 实际版本 */
    private String actualVersion;
    /** 运行槽位 */
    private String slot;
    /** 是否活动槽 */
    private Boolean active;
    /** 结果：SUCCESS/FAILED/ROLLED_BACK */
    private String result;
    /** 失败阶段 */
    private String failureStage;
    /** 回滚结果：SUCCESS/FAILED/ROLLED_BACK */
    private String rollbackResult;
    /** 关联软件包ID */
    private String packageId;
}
