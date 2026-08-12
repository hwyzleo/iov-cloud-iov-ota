package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日志上传授权命令（CR-012 §5.8、US-082）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAuthCmd {

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 采集范围 */
    private String logScope;

    /** 脱敏版本 */
    private String desensitizeVersion;

    /** 车架号 */
    private String vin;
}
