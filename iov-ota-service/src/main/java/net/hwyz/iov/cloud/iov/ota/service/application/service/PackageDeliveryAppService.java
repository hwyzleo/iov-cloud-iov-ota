package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
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
    /** 必需包集合唯一投影来源（与 TaskCheck 下发的 package_list 同源） */
    private final VehicleTaskPackageService vehicleTaskPackageService;

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

        if ("DOWNLOAD".equals(cmd.getOperation())
                && vt.getStatus() == VehicleTaskStatus.DOWNLOAD_PENDING) {
            // 幂等：仅在待下载状态迁移。已 IN_PROGRESS / 已 READY_TO_INSTALL 时
            // 不得再调 startDownload()（会抛状态异常 → 不回执 → 车端超时）。
            vt.startDownload();
            vehicleTaskRepository.save(vt);
        } else if ("DOWNLOAD".equals(cmd.getOperation())) {
            log.info("车辆任务[{}]状态[{}]不需迁移下载态，继续签发凭证",
                    cmd.getVehicleTaskId(), vt.getStatus());
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

        // 全部必需包校验成功后 VehicleTask 进入 READY_TO_INSTALL。
        // 关键：必须与**必需包集合**比对，不能只看已上报结果——否则第 1 个包
        // DECRYPT SUCCESS 就会把任务提前置为 READY_TO_INSTALL，后续包的
        // 下载授权会因状态校验异常而不回执（实车 2026-11-12）。
        if ("SUCCESS".equals(cmd.getResultStatus()) && "DECRYPT".equals(cmd.getStage())) {
            VehicleTask vt = vehicleTaskRepository
                    .getById(VehicleTaskId.of(cmd.getVehicleTaskId())).orElse(null);
            if (vt != null && vt.getDownloadReadyState() != DownloadReadyState.VERIFIED) {
                java.util.Set<String> required =
                        vehicleTaskPackageService.requiredPackageIds(vt);
                java.util.Set<String> decryptSucceeded =
                        packageStageResultMapper.selectByVehicleTaskId(cmd.getVehicleTaskId()).stream()
                                .filter(r -> "DECRYPT".equals(r.getStage()))
                                .filter(r -> "SUCCESS".equals(r.getResultStatus()))
                                .map(PackageStageResultPo::getPackageId)
                                .collect(java.util.stream.Collectors.toSet());
                if (required.isEmpty()) {
                    log.warn("车辆任务[{}]必需包集合为空，不做下载就绪判定", cmd.getVehicleTaskId());
                } else if (decryptSucceeded.containsAll(required)) {
                    log.info("车辆任务[{}]全部必需包({})校验成功，进入下载就绪",
                            cmd.getVehicleTaskId(), required.size());
                    vt.markDownloadReady();
                    vehicleTaskRepository.save(vt);
                } else {
                    log.info("车辆任务[{}]下载未完成：已完成{}/{}，待完成{}",
                            cmd.getVehicleTaskId(),
                            required.stream().filter(decryptSucceeded::contains).count(),
                            required.size(),
                            required.stream().filter(id -> !decryptSucceeded.contains(id)).toList());
                }
            }
        }
    }
}
