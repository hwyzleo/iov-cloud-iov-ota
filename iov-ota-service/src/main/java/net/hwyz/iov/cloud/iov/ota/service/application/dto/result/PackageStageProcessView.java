package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

/**
 * 包下载/验签/解密阶段（CR-015 §3.3 packageStages）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class PackageStageProcessView {

    private String packageId;
    private String packageRevision;
    private String etag;
    private String downloadState;
    private String verifyState;
    private String signatureResult;
    private String decryptResult;
    private String stageResultStatus;
    private String failReason;
}
