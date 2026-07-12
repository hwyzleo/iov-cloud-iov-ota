package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwareBuildVersionState;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaBaseException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 软件内部版本聚合根状态流转测试 (CR-004)
 */
@DisplayName("SoftwareBuildVersion 状态流转")
class SoftwareBuildVersionTest {

    private SoftwareBuildVersion buildSbv(String state) {
        return SoftwareBuildVersion.builder()
                .id(new SoftwareBuildVersionId(1L))
                .deviceCode(new DeviceCode("TBOX"))
                .softwarePn(new SoftwarePn("S00000001AA"))
                .softwareBuildVer("V1.0.0")
                .buildState(state)
                .build();
    }

    @Nested
    @DisplayName("release() 发布")
    class Release {

        @Test
        @DisplayName("DRAFT -> RELEASED 成功")
        void draftToReleased() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.DRAFT.name());
            sbv.release();
            assertEquals(SoftwareBuildVersionState.RELEASED.name(), sbv.getBuildState());
            assertNotNull(sbv.getReleaseTime());
        }

        @Test
        @DisplayName("TESTING -> RELEASED 成功")
        void testingToReleased() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.TESTING.name());
            sbv.release();
            assertEquals(SoftwareBuildVersionState.RELEASED.name(), sbv.getBuildState());
        }

        @Test
        @DisplayName("RELEASED -> RELEASED 失败")
        void releasedToReleasedFails() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.RELEASED.name());
            assertThrows(OtaBaseException.class, sbv::release);
        }

        @Test
        @DisplayName("DEPRECATED -> RELEASED 失败")
        void deprecatedToReleasedFails() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.DEPRECATED.name());
            assertThrows(OtaBaseException.class, sbv::release);
        }

        @Test
        @DisplayName("RETIRED -> RELEASED 失败")
        void retiredToReleasedFails() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.RETIRED.name());
            assertThrows(OtaBaseException.class, sbv::release);
        }
    }

    @Nested
    @DisplayName("deprecate() 停用")
    class Deprecate {

        @Test
        @DisplayName("RELEASED -> DEPRECATED 成功")
        void releasedToDeprecated() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.RELEASED.name());
            sbv.deprecate();
            assertEquals(SoftwareBuildVersionState.DEPRECATED.name(), sbv.getBuildState());
        }

        @Test
        @DisplayName("DRAFT -> DEPRECATED 失败")
        void draftToDeprecatedFails() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.DRAFT.name());
            assertThrows(OtaBaseException.class, sbv::deprecate);
        }
    }

    @Nested
    @DisplayName("retire() 退役")
    class Retire {

        @Test
        @DisplayName("DEPRECATED -> RETIRED 成功")
        void deprecatedToRetired() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.DEPRECATED.name());
            sbv.retire();
            assertEquals(SoftwareBuildVersionState.RETIRED.name(), sbv.getBuildState());
        }

        @Test
        @DisplayName("RELEASED -> RETIRED 失败")
        void releasedToRetiredFails() {
            SoftwareBuildVersion sbv = buildSbv(SoftwareBuildVersionState.RELEASED.name());
            assertThrows(OtaBaseException.class, sbv::retire);
        }
    }

    @Test
    @DisplayName("isReleased() 判断")
    void isReleased() {
        assertTrue(buildSbv(SoftwareBuildVersionState.RELEASED.name()).isReleased());
        assertFalse(buildSbv(SoftwareBuildVersionState.DRAFT.name()).isReleased());
        assertFalse(buildSbv(SoftwareBuildVersionState.DEPRECATED.name()).isReleased());
    }
}
