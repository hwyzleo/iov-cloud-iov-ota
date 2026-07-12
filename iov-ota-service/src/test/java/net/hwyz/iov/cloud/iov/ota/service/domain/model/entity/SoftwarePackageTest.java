package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwarePackageState;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaBaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 软件包制品状态流转测试 (CR-004)
 */
@DisplayName("SoftwarePackage 制品状态流转")
class SoftwarePackageTest {

    private SoftwarePackage buildPackage(String state) {
        return SoftwarePackage.builder()
                .packageState(state)
                .build();
    }

    @Nested
    @DisplayName("deprecate() 停用")
    class Deprecate {

        @Test
        @DisplayName("ACTIVE -> DEPRECATED 成功")
        void activeToDeprecated() {
            SoftwarePackage pkg = buildPackage(SoftwarePackageState.ACTIVE.name());
            pkg.deprecate();
            assertEquals(SoftwarePackageState.DEPRECATED.name(), pkg.getPackageState());
        }

        @Test
        @DisplayName("REVOKED -> DEPRECATED 失败")
        void revokedToDeprecatedFails() {
            SoftwarePackage pkg = buildPackage(SoftwarePackageState.REVOKED.name());
            assertThrows(OtaBaseException.class, pkg::deprecate);
        }
    }

    @Nested
    @DisplayName("revoke() 吊销")
    class Revoke {

        @Test
        @DisplayName("ACTIVE -> REVOKED 成功")
        void activeToRevoked() {
            SoftwarePackage pkg = buildPackage(SoftwarePackageState.ACTIVE.name());
            pkg.revoke();
            assertEquals(SoftwarePackageState.REVOKED.name(), pkg.getPackageState());
        }

        @Test
        @DisplayName("DEPRECATED -> REVOKED 成功")
        void deprecatedToRevoked() {
            SoftwarePackage pkg = buildPackage(SoftwarePackageState.DEPRECATED.name());
            pkg.revoke();
            assertEquals(SoftwarePackageState.REVOKED.name(), pkg.getPackageState());
        }

        @Test
        @DisplayName("RETIRED -> REVOKED 失败")
        void retiredToRevokedFails() {
            SoftwarePackage pkg = buildPackage(SoftwarePackageState.RETIRED.name());
            assertThrows(OtaBaseException.class, pkg::revoke);
        }
    }

    @Nested
    @DisplayName("retire() 退役")
    class Retire {

        @Test
        @DisplayName("DEPRECATED -> RETIRED 成功")
        void deprecatedToRetired() {
            SoftwarePackage pkg = buildPackage(SoftwarePackageState.DEPRECATED.name());
            pkg.retire();
            assertEquals(SoftwarePackageState.RETIRED.name(), pkg.getPackageState());
        }

        @Test
        @DisplayName("REVOKED -> RETIRED 成功")
        void revokedToRetired() {
            SoftwarePackage pkg = buildPackage(SoftwarePackageState.REVOKED.name());
            pkg.retire();
            assertEquals(SoftwarePackageState.RETIRED.name(), pkg.getPackageState());
        }

        @Test
        @DisplayName("ACTIVE -> RETIRED 失败")
        void activeToRetiredFails() {
            SoftwarePackage pkg = buildPackage(SoftwarePackageState.ACTIVE.name());
            assertThrows(OtaBaseException.class, pkg::retire);
        }
    }

    @Test
    @DisplayName("isActive() 判断")
    void isActive() {
        assertTrue(buildPackage(SoftwarePackageState.ACTIVE.name()).isActive());
        assertFalse(buildPackage(SoftwarePackageState.DEPRECATED.name()).isActive());
        assertFalse(buildPackage(SoftwarePackageState.REVOKED.name()).isActive());
        assertFalse(buildPackage(SoftwarePackageState.RETIRED.name()).isActive());
    }
}
