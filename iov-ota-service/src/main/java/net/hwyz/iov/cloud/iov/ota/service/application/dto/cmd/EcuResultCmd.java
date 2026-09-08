package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ECU 结果命令（CR-012 §5.7 / CR-019 §4.23.5）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcuResultCmd {

    private String ecuId;
    /** ECU 软件模型：SINGLE_IMAGE/MULTI_TARGET */
    private String softwareModel;
    private String targetSoftwareVersion;
    private String actualSoftwareVersion;
    /** 结果：SUCCESS/FAILED/ROLLED_BACK */
    private String result;
    private String failReason;
    /** per-Target/Slot 软件单元结果（MULTI_TARGET 时非空） */
    private List<SoftwareUnitResultCmd> softwareUnitResults;
}
