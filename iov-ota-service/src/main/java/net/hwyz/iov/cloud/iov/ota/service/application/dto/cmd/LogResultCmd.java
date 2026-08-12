package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日志上传结果命令（CR-012 §5.8、US-082）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResultCmd {

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 日志上传申请ID */
    private String logRequestId;

    /** 对象存储键 */
    private String objectKey;

    /** 日志摘要 */
    private String logDigest;

    /** 上传结果：SUCCESS / FAIL */
    private String uploadResult;

    /** 车架号 */
    private String vin;
}
