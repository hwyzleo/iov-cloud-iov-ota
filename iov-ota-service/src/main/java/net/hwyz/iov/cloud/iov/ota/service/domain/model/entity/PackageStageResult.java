package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * 包阶段结果实体（CR-012 §3、§5.4）
 *
 * <p>下载/验签/解密终态；UK(stage_result_id)，另存 digest 防冲突。
 * 下载终态以 stageResultId 幂等落库；成功需要摘要、签名以及加密包解密结果均满足。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class PackageStageResult {

    private final Long id;
    /** 阶段结果ID（幂等键） */
    private final String stageResultId;
    private final Long vehicleTaskId;
    private final String packageId;
    /** 阶段：DOWNLOAD / VERIFY / DECRYPT */
    private final String stage;
    /** 结果状态：SUCCESS / FAILED */
    private final String resultStatus;
    /** 包版本号 */
    private final String packageRevision;
    /** ETag */
    private final String etag;
    /** 包摘要 */
    private final String digest;
    /** 签名校验结果 */
    private final String signatureResult;
    /** 解密结果 */
    private final String decryptResult;
    /** 失败原因 */
    private final String failReason;
}
