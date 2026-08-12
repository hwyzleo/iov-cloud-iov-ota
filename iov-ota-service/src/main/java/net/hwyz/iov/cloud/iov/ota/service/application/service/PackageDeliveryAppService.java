package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DownloadAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.StageResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DownloadAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.PackageCredentialService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PackageStageResultMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageStageResultPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskPackagePo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 包投递应用服务（CR-012 §5.4、US-078）
 *
 * <p>包版本校验、短期凭证签发、下载阶段结果受理。
 * 预签名凭证只在 DOWNLOAD/RESUME/RESET_OFFSET 时返回。
 * ETag 或 packageRevision 变化时 offset 清零。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageDeliveryAppService {

    private final VehicleTaskRepository vehicleTaskRepository;
    private final PackageCredentialService packageCredentialService;
    private final PackageStageResultMapper packageStageResultMapper;
    private final VehicleTaskPackageMapper vehicleTaskPackageMapper;

    /**
     * 签发下载预签名凭证。
     *
     * @param cmd 下载授权命令
     * @return 下载授权结果
     */
    public DownloadAuthResult authorizeDownload(DownloadAuthCmd cmd) {
        log.info("车辆[{}]申请下载凭证，车辆任务[{}]，包[{}]", cmd.getVin(), cmd.getVehicleTaskId(), cmd.getPackageId());

        VehicleTask vt = vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId()))
                .orElseThrow(() -> new IllegalStateException("车辆任务[" + cmd.getVehicleTaskId() + "]不存在"));

        // 检查 ETag 变化，决定是否 RESET_OFFSET
        boolean resetOffset = false;
        long offset = cmd.getOffset() != null ? cmd.getOffset() : 0L;

        List<VehicleTaskPackagePo> packages = vehicleTaskPackageMapper.selectByVehicleTaskId(cmd.getVehicleTaskId());
        for (VehicleTaskPackagePo pkg : packages) {
            if (cmd.getPackageId().equals(pkg.getPackageId())) {
                if (cmd.getEtag() != null && !cmd.getEtag().equals(pkg.getEtag())) {
                    resetOffset = true;
                    offset = 0L;
                }
                break;
            }
        }

        if ("DOWNLOAD".equals(cmd.getOperation())) {
            vt.startDownload();
            vehicleTaskRepository.save(vt);
        }

        PackageCredentialService.PackageCredential credential =
                packageCredentialService.signDownloadCredential(
                        cmd.getPackageId(), cmd.getPackageRevision(), cmd.getEtag(),
                        offset, Duration.ofMinutes(30));

        return DownloadAuthResult.builder()
                .presignedUrl(credential.presignedUrl())
                .credentialToken(credential.credentialToken())
                .expiresAt(credential.expiresAt())
                .offset(credential.offset())
                .resetOffset(resetOffset || credential.resetOffset())
                .packageRevision(cmd.getPackageRevision())
                .build();
    }

    /**
     * 受理下载阶段结果（幂等）。
     *
     * @param cmd 阶段结果命令
     */
    @Transactional
    public void submitStageResult(StageResultCmd cmd) {
        log.info("车辆[{}]提交包阶段结果，车辆任务[{}]，包[{}]，阶段[{}]",
                cmd.getVin(), cmd.getVehicleTaskId(), cmd.getPackageId(), cmd.getStage());

        // 幂等：stageResultId 已存在则跳过
        PackageStageResultPo existing = packageStageResultMapper.selectByStageResultId(cmd.getStageResultId());
        if (existing != null) {
            log.info("包阶段结果[{}]已存在，幂等跳过", cmd.getStageResultId());
            return;
        }

        PackageStageResultPo po = PackageStageResultPo.builder()
                .stageResultId(cmd.getStageResultId())
                .vehicleTaskId(cmd.getVehicleTaskId())
                .packageId(cmd.getPackageId())
                .stage(cmd.getStage())
                .resultStatus(cmd.getResultStatus())
                .packageRevision(cmd.getPackageRevision())
                .etag(cmd.getEtag())
                .digest(cmd.getDigest())
                .signatureResult(cmd.getSignatureResult())
                .decryptResult(cmd.getDecryptResult())
                .failReason(cmd.getFailReason())
                .build();
        packageStageResultMapper.insert(po);

        // 全部必需包校验成功后 VehicleTask 进入 READY_TO_INSTALL
        if ("SUCCESS".equals(cmd.getResultStatus()) && "DECRYPT".equals(cmd.getStage())) {
            List<PackageStageResultPo> allResults = packageStageResultMapper.selectByVehicleTaskId(cmd.getVehicleTaskId());
            boolean allSucceeded = allResults.stream()
                    .filter(r -> "DECRYPT".equals(r.getStage()))
                    .allMatch(r -> "SUCCESS".equals(r.getResultStatus()));
            if (allSucceeded && !allResults.isEmpty()) {
                VehicleTask vt = vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId())).orElse(null);
                if (vt != null && vt.getDownloadReadyState() != DownloadReadyState.VERIFIED) {
                    vt.markDownloadReady();
                    vehicleTaskRepository.save(vt);
                }
            }
        }
    }
}
