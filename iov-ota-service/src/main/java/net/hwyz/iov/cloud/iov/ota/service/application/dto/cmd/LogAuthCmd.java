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

    /** 安装执行ID（可空，CR-026） */
    private Long executionId;

    /** 车端日志申请ID（作为 EDD-OSS requestId 幂等键，CR-026） */
    private String logRequestId;

    /** 原始文件名（CR-026） */
    private String fileName;

    /** 声明 Content-Type（CR-026） */
    private String contentType;

    /** 声明字节数（CR-026） */
    private Long sizeBytes;

    /** 声明 SHA-256（64 位小写十六进制，CR-026） */
    private String sha256Hex;

    /** 隐私分级（CR-026） */
    private String privacyLevel;

    /** 上传授权 TTL 秒（可空，CR-026） */
    private Integer ttlSeconds;

    /** 采集范围 */
    private String logScope;

    /** 脱敏版本 */
    private String desensitizeVersion;

    /** 车架号 */
    private String vin;
}
