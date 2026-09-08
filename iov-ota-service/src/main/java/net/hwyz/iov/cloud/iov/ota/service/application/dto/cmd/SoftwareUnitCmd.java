package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 清单软件单元命令（CR-019 §4.1）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareUnitCmd {

    /** 软件目标编码（存量规范化为 ECU_IMAGE） */
    private String softwareTargetCode;
    /** 软件零件号 */
    private String softwarePartNumber;
    /** 软件版本 */
    private String swVersion;
    /** 运行槽位（可空） */
    private String slot;
    /** 是否活动槽 */
    private Boolean active;
    /** 软件单元内容摘要（可空） */
    private String digest;
}
