package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwareBuildVersionState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwarePackageState;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwareBuildVersionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.SoftwareBuildVersionDomainService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 软件内部版本应用服务测试 (CR-004)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SoftwareBuildVersionAppService CR-004")
class SoftwareBuildVersionAppServiceTest {

    @Mock private SoftwareBuildVersionRepository softwareBuildVersionRepository;
    @Mock private SoftwareBuildVersionDomainService softwareBuildVersionDomainService;
    @Mock private SoftwareBuildVersionMapper softwareBuildVersionMapper;
    @Mock private SoftwarePackageAppService softwarePackageAppService;
    @Mock private SoftwareBuildVersionPackageMapper softwareBuildVersionPackageMapper;
    @Mock private SoftwareBuildVersionDependencyMapper softwareBuildVersionDependencyMapper;
    @Mock private SoftwareBuildVersionTestReportMapper softwareBuildVersionTestReportMapper;
    @Mock private SoftwareBuildVersionAdaptationMapper softwareBuildVersionAdaptationMapper;

    @InjectMocks
    private SoftwareBuildVersionAppService appService;

    @Nested
    @DisplayName("createSoftwareBuildVersion - DRAFT 默认值")
    class CreateWithDefaultState {

        @Test
        @DisplayName("未设置 buildState 时默认 DRAFT")
        void defaultsToDraft() {
            SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
            po.setDeviceCode("TBOX");
            po.setBuildState(null);

            appService.createSoftwareBuildVersion(po);

            assertEquals(SoftwareBuildVersionState.DRAFT.name(), po.getBuildState());
            verify(softwareBuildVersionMapper).insertPo(po);
        }

        @Test
        @DisplayName("已设置 buildState 时保留原值")
        void preservesExistingState() {
            SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
            po.setBuildState(SoftwareBuildVersionState.RELEASED.name());

            appService.createSoftwareBuildVersion(po);

            assertEquals(SoftwareBuildVersionState.RELEASED.name(), po.getBuildState());
        }
    }

    @Nested
    @DisplayName("releaseSoftwareBuildVersion - 状态流转")
    class ReleaseSbv {

        @Test
        @DisplayName("DRAFT -> RELEASED 成功")
        void draftToReleased() {
            SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
            po.setId(1L);
            po.setBuildState(SoftwareBuildVersionState.DRAFT.name());
            when(softwareBuildVersionMapper.selectPoById(1L)).thenReturn(po);

            appService.releaseSoftwareBuildVersion(1L);

            assertEquals(SoftwareBuildVersionState.RELEASED.name(), po.getBuildState());
            assertNotNull(po.getReleaseTime());
            verify(softwareBuildVersionMapper).updatePo(po);
        }

        @Test
        @DisplayName("RELEASED -> RELEASED 失败")
        void releasedToReleasedFails() {
            SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
            po.setId(1L);
            po.setBuildState(SoftwareBuildVersionState.RELEASED.name());
            when(softwareBuildVersionMapper.selectPoById(1L)).thenReturn(po);

            assertThrows(IllegalStateException.class, () -> appService.releaseSoftwareBuildVersion(1L));
        }

        @Test
        @DisplayName("版本不存在时抛异常")
        void notExistFails() {
            when(softwareBuildVersionMapper.selectPoById(999L)).thenReturn(null);
            assertThrows(IllegalArgumentException.class, () -> appService.releaseSoftwareBuildVersion(999L));
        }
    }

    @Nested
    @DisplayName("checkReleaseGate - 双重发布门禁")
    class ReleaseGate {

        @Test
        @DisplayName("版本 RELEASED + 所有包 ACTIVE -> 通过")
        void releasedAndAllActivePasses() {
            SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
            po.setId(1L);
            po.setBuildState(SoftwareBuildVersionState.RELEASED.name());
            when(softwareBuildVersionMapper.selectPoById(1L)).thenReturn(po);

            SoftwarePackagePo pkg1 = new SoftwarePackagePo();
            pkg1.setPackageState(SoftwarePackageState.ACTIVE.name());
            SoftwarePackagePo pkg2 = new SoftwarePackagePo();
            pkg2.setPackageState(SoftwarePackageState.ACTIVE.name());
            when(softwarePackageAppService.listBySoftwareBuildVersionId(1L)).thenReturn(List.of(pkg1, pkg2));

            assertTrue(appService.checkReleaseGate(1L));
        }

        @Test
        @DisplayName("版本非 RELEASED -> 不通过")
        void notReleasedFails() {
            SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
            po.setBuildState(SoftwareBuildVersionState.DRAFT.name());
            when(softwareBuildVersionMapper.selectPoById(1L)).thenReturn(po);

            assertFalse(appService.checkReleaseGate(1L));
        }

        @Test
        @DisplayName("版本 RELEASED + 一个包 DEPRECATED -> 不通过")
        void releasedButPackageDeprecatedFails() {
            SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
            po.setId(1L);
            po.setBuildState(SoftwareBuildVersionState.RELEASED.name());
            when(softwareBuildVersionMapper.selectPoById(1L)).thenReturn(po);

            SoftwarePackagePo activePkg = new SoftwarePackagePo();
            activePkg.setPackageState(SoftwarePackageState.ACTIVE.name());
            SoftwarePackagePo deprecatedPkg = new SoftwarePackagePo();
            deprecatedPkg.setPackageState(SoftwarePackageState.DEPRECATED.name());
            when(softwarePackageAppService.listBySoftwareBuildVersionId(1L)).thenReturn(List.of(activePkg, deprecatedPkg));

            assertFalse(appService.checkReleaseGate(1L));
        }

        @Test
        @DisplayName("版本 RELEASED + 一个包 REVOKED -> 不通过")
        void releasedButPackageRevokedFails() {
            SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
            po.setId(1L);
            po.setBuildState(SoftwareBuildVersionState.RELEASED.name());
            when(softwareBuildVersionMapper.selectPoById(1L)).thenReturn(po);

            SoftwarePackagePo revokedPkg = new SoftwarePackagePo();
            revokedPkg.setPackageState(SoftwarePackageState.REVOKED.name());
            when(softwarePackageAppService.listBySoftwareBuildVersionId(1L)).thenReturn(List.of(revokedPkg));

            assertFalse(appService.checkReleaseGate(1L));
        }

        @Test
        @DisplayName("版本不存在 -> 不通过")
        void notExistFails() {
            when(softwareBuildVersionMapper.selectPoById(999L)).thenReturn(null);
            assertFalse(appService.checkReleaseGate(999L));
        }
    }

    @Nested
    @DisplayName("saveAdaptations - 适配矩阵全量替换")
    class SaveAdaptations {

        @Test
        @DisplayName("先删后增，全量替换")
        void replaceAll() {
            appService.saveAdaptations(1L, List.of());

            verify(softwareBuildVersionAdaptationMapper).deletePoBySbvId(1L);
            verify(softwareBuildVersionAdaptationMapper, never()).batchInsertPo(any());
        }
    }
}
