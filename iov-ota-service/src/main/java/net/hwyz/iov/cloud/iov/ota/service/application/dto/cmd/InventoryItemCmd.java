package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 清单明细项命令（CR-012 §5.1）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemCmd {

    private String ecuId;
    private String ecuName;
    private String softwarePn;
    private String softwareVersion;
    private String hardwarePn;
    private String hardwareVersion;
}
