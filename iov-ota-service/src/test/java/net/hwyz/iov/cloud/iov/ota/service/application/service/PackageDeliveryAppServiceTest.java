package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DownloadAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.StageResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DownloadAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.PackageCredentialService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PackageStageResultMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageStageResultPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 包投递服务回归测试。
 *
 * <p>实车缺陷（ORIN 2026-11-12）：第 1 个包 DECRYPT SUCCESS 后云端把车辆任务提前置为
 * READY_TO_INSTALL（旧实现只检查"已上报结果全部成功"），导致后续包下载授权里
 * {@code startDownload()} 状态校验抛异常 → handler 静默不回执 → 车端 2.5s 超时，
 * FOTA 流程卡死在第 2 个包。</p>
 */
@DisplayName("PackageDeliveryAppService - 必需包判定与下载授权幂等")
class PackageDeliveryAppServiceTest {

    private final VehicleTaskRepository vehicleTaskRepository = mock(VehicleTaskRepository.class);
    private final PackageCredentialService packageCredentialService = mock(PackageCredentialService.class);
    private final PackageStageResultMapper packageStageResultMapper = mock(PackageStageResultMapper.class);
    private final VehicleTaskPackageMapper vehicleTaskPackageMapper = mock(VehicleTaskPackageMapper.class);
    private final VehicleTaskPackageService vehicleTaskPackageService = mock(VehicleTaskPackageService.class);
    private final net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PackageDownloadProgressMapper
            packageDownloadProgressMapper = mock(
            net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PackageDownloadProgressMapper.class);

    private PackageDeliveryAppService service;

    @BeforeEach
    void setUp() {
        service = new PackageDeliveryAppService(vehicleTaskRepository, packageCredentialService,
                packageStageResultMapper, vehicleTaskPackageMapper, vehicleTaskPackageService,
                packageDownloadProgressMapper);
        when(vehicleTaskPackageMapper.selectByVehicleTaskId(any())).thenReturn(List.of());
        when(packageCredentialService.signDownloadCredential(any(), any(), any(), anyLong(), any(Duration.class)))
                .thenReturn(new PackageCredentialService.PackageCredential(
                        "https://oss/presigned", "token", Instant.now().plusSeconds(1800), 0L, false));
    }

    private DownloadAuthCmd downloadCmd(String packageId) {
        return DownloadAuthCmd.builder()
                .vin("HWYZTEST900000001").vehicleTaskId(3L)
                .packageId(packageId).packageRevision("v1.0")
                .offset(0L).operation("DOWNLOAD").build();
    }

    private StageResultCmd decryptCmd(String packageId) {
        return StageResultCmd.builder()
                .vehicleTaskId(3L).stageResultId("SR-" + packageId)
                .packageId(packageId).stage("DECRYPT").resultStatus("SUCCESS").build();
    }

    private PackageStageResultPo decryptResult(String packageId) {
        return PackageStageResultPo.builder()
                .vehicleTaskId(3L).packageId(packageId).stage("DECRYPT").resultStatus("SUCCESS").build();
    }

    @Test
    @DisplayName("任务已 READY_TO_INSTALL 时再次申请下载凭证：不迁移状态、不抛异常、照常签发")
    void authorizeDownload_isIdempotent_whenTaskAlreadyReadyToInstall() {
        VehicleTask vt = mock(VehicleTask.class);
        when(vt.getStatus()).thenReturn(VehicleTaskStatus.READY_TO_INSTALL);
        when(vehicleTaskRepository.getById(any())).thenReturn(Optional.of(vt));

        DownloadAuthResult result = assertDoesNotThrow(
                () -> service.authorizeDownload(downloadCmd("PKG-B")));

        verify(vt, never()).startDownload();
        assertEquals("https://oss/presigned", result.getPresignedUrl());
    }

    @Test
    @DisplayName("仅部分必需包上报 DECRYPT SUCCESS：不得进入 READY_TO_INSTALL")
    void submitStageResult_partialPackages_mustNotMarkReady() {
        VehicleTask vt = mock(VehicleTask.class);
        when(vt.getDownloadReadyState()).thenReturn(DownloadReadyState.IN_PROGRESS);
        when(vehicleTaskRepository.getById(any())).thenReturn(Optional.of(vt));
        when(vehicleTaskPackageService.requiredPackageIds(vt))
                .thenReturn(Set.of("PKG-A", "PKG-B", "PKG-C", "PKG-D"));
        when(packageStageResultMapper.selectByVehicleTaskIdAndStageResultId(any(), anyString()))
                .thenReturn(null);
        when(packageStageResultMapper.selectByVehicleTaskId(3L))
                .thenReturn(List.of(decryptResult("PKG-A")));

        service.submitStageResult(decryptCmd("PKG-A"));

        verify(vt, never()).markDownloadReady();
    }

    @Test
    @DisplayName("全部必需包都上报 DECRYPT SUCCESS：进入 READY_TO_INSTALL")
    void submitStageResult_allPackages_markReady() {
        VehicleTask vt = mock(VehicleTask.class);
        when(vt.getDownloadReadyState()).thenReturn(DownloadReadyState.IN_PROGRESS);
        when(vehicleTaskRepository.getById(any())).thenReturn(Optional.of(vt));
        when(vehicleTaskPackageService.requiredPackageIds(vt))
                .thenReturn(Set.of("PKG-A", "PKG-B", "PKG-C", "PKG-D"));
        when(packageStageResultMapper.selectByVehicleTaskIdAndStageResultId(any(), anyString()))
                .thenReturn(null);
        when(packageStageResultMapper.selectByVehicleTaskId(3L)).thenReturn(List.of(
                decryptResult("PKG-A"), decryptResult("PKG-B"),
                decryptResult("PKG-C"), decryptResult("PKG-D")));

        service.submitStageResult(decryptCmd("PKG-D"));

        verify(vt, times(1)).markDownloadReady();
    }

    @Test
    @DisplayName("必需包集合无法投影（空）：不做下载就绪判定，避免误判")
    void submitStageResult_emptyRequiredSet_doesNotMarkReady() {
        VehicleTask vt = mock(VehicleTask.class);
        when(vt.getDownloadReadyState()).thenReturn(DownloadReadyState.IN_PROGRESS);
        when(vehicleTaskRepository.getById(any())).thenReturn(Optional.of(vt));
        when(vehicleTaskPackageService.requiredPackageIds(vt)).thenReturn(Set.of());
        when(packageStageResultMapper.selectByVehicleTaskIdAndStageResultId(any(), anyString()))
                .thenReturn(null);
        when(packageStageResultMapper.selectByVehicleTaskId(3L))
                .thenReturn(List.of(decryptResult("PKG-A")));

        service.submitStageResult(decryptCmd("PKG-A"));

        verify(vt, never()).markDownloadReady();
    }

    @Test
    @DisplayName("车辆任务重置后同 stage_result_id 不得误判重复（幂等键按车辆任务隔离）")
    void submitStageResult_sameStageResultIdOnNewVehicleTask_isRecorded() {
        // 实车 2026-11-15：车辆任务 3 EXECUTING 重置为车辆任务 4，车端复用确定性 stage_result_id，
        // 旧实现按全局 ID 判重 → 新任务 4 个包 DECRYPT SUCCESS 全被丢弃 → 永远 DOWNLOAD_PENDING。
        VehicleTask vt = mock(VehicleTask.class);
        when(vt.getDownloadReadyState()).thenReturn(DownloadReadyState.IN_PROGRESS);
        when(vehicleTaskRepository.getById(any())).thenReturn(Optional.of(vt));
        when(vehicleTaskPackageService.requiredPackageIds(vt)).thenReturn(Set.of("PKG-A"));
        // 同 stage_result_id 在车辆任务 3 已存在（全局查询会命中），但车辆任务 4 不存在
        when(packageStageResultMapper.selectByStageResultId("SR-PKG-A"))
                .thenReturn(PackageStageResultPo.builder()
                        .vehicleTaskId(3L).packageId("PKG-A").stage("DECRYPT")
                        .resultStatus("SUCCESS").stageResultId("SR-PKG-A").build());
        when(packageStageResultMapper.selectByVehicleTaskIdAndStageResultId(4L, "SR-PKG-A"))
                .thenReturn(null);
        when(packageStageResultMapper.selectByVehicleTaskId(4L))
                .thenReturn(List.of(PackageStageResultPo.builder()
                        .vehicleTaskId(4L).packageId("PKG-A").stage("DECRYPT")
                        .resultStatus("SUCCESS").stageResultId("SR-PKG-A").build()));

        service.submitStageResult(StageResultCmd.builder()
                .vehicleTaskId(4L).stageResultId("SR-PKG-A")
                .packageId("PKG-A").stage("DECRYPT").resultStatus("SUCCESS").build());

        verify(packageStageResultMapper).insert(any(PackageStageResultPo.class));
        verify(vt, times(1)).markDownloadReady();
    }

    @Test
    @DisplayName("未加密包只上报 DOWNLOAD SUCCESS（无 DECRYPT 阶段）：全部必需包完成后仍进入 READY_TO_INSTALL")
    void submitStageResult_withoutDecryptStage_markReadyWhenAllRequiredCompleted() {
        // 云端下载授权未下发加密材料，未加密包的车端不会产生 DECRYPT 阶段；
        // 旧实现把 DECRYPT SUCCESS 当作唯一触发条件 → 非加密包任务永远停在 DOWNLOAD_PENDING。
        VehicleTask vt = mock(VehicleTask.class);
        when(vt.getDownloadReadyState()).thenReturn(DownloadReadyState.IN_PROGRESS);
        when(vehicleTaskRepository.getById(any())).thenReturn(Optional.of(vt));
        when(vehicleTaskPackageService.requiredPackageIds(vt)).thenReturn(Set.of("PKG-A", "PKG-B"));
        when(packageStageResultMapper.selectByVehicleTaskId(3L)).thenReturn(List.of(
                PackageStageResultPo.builder().vehicleTaskId(3L).packageId("PKG-A")
                        .stage("DOWNLOAD").resultStatus("SUCCESS").build(),
                PackageStageResultPo.builder().vehicleTaskId(3L).packageId("PKG-B")
                        .stage("DOWNLOAD").resultStatus("SUCCESS").build()));

        service.submitStageResult(StageResultCmd.builder()
                .vehicleTaskId(3L).stageResultId("SR-PKG-B-DL")
                .packageId("PKG-B").stage("DOWNLOAD").resultStatus("SUCCESS").build());

        verify(vt, times(1)).markDownloadReady();
    }

    @Test
    @DisplayName("加密包最高阶段 DECRYPT FAILED：即使已有 VERIFY SUCCESS 也不得进入 READY_TO_INSTALL")
    void submitStageResult_decryptFailed_mustNotMarkReady() {
        VehicleTask vt = mock(VehicleTask.class);
        when(vt.getDownloadReadyState()).thenReturn(DownloadReadyState.IN_PROGRESS);
        when(vehicleTaskRepository.getById(any())).thenReturn(Optional.of(vt));
        when(vehicleTaskPackageService.requiredPackageIds(vt)).thenReturn(Set.of("PKG-A"));
        when(packageStageResultMapper.selectByVehicleTaskId(3L)).thenReturn(List.of(
                PackageStageResultPo.builder().vehicleTaskId(3L).packageId("PKG-A")
                        .stage("VERIFY").resultStatus("SUCCESS").build(),
                PackageStageResultPo.builder().vehicleTaskId(3L).packageId("PKG-A")
                        .stage("DECRYPT").resultStatus("FAILED").build()));

        service.submitStageResult(StageResultCmd.builder()
                .vehicleTaskId(3L).stageResultId("SR-PKG-A-DEC")
                .packageId("PKG-A").stage("DECRYPT").resultStatus("FAILED").build());

        verify(vt, never()).markDownloadReady();
    }
}
