package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ECU 结果命令（CR-012 §5.7）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcuResultCmd {

    private String ecuId;
    private String targetSoftwareVersion;
    private String actualSoftwareVersion;
    /** 结果：SUCCESS/FAILED/ROLLED_BACK */
    private String result;
    private String failReason;
}
