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

    /** 车端上报的对象ETag（仅登记，CR-026） */
    private String etag;

    /** 车端上报的摘要算法（CR-026） */
    private String digestAlgorithm;

    /** 车端上报的上传完成时间（epoch ms，未知为 0/null，CR-026） */
    private Long uploadedAtMs;

    /** 车端上报的错误码（可空，CR-026） */
    private String errorCode;

    /** 车架号 */
    private String vin;
}
