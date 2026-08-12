package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 包阶段结果命令（CR-012 §5.4、US-078）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageResultCmd {

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 阶段结果ID（幂等键） */
    private String stageResultId;

    /** 包ID */
    private String packageId;

    /** 阶段：DOWNLOAD / VERIFY / DECRYPT */
    private String stage;

    /** 结果状态：SUCCESS / FAILED */
    private String resultStatus;

    /** 包版本号 */
    private String packageRevision;

    /** 对象 ETag */
    private String etag;

    /** 包摘要 */
    private String digest;

    /** 签名校验结果 */
    private String signatureResult;

    /** 解密结果 */
    private String decryptResult;

    /** 失败原因 */
    private String failReason;

    /** 车架号 */
    private String vin;
}
