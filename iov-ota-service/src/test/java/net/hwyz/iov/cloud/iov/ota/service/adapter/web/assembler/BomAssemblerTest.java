package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.BomSoftwareBuildVersionOapi;
import net.hwyz.iov.cloud.iov.ota.api.vo.BomSoftwarePackageOapi;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwareBuildVersionState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.SoftwarePackageState;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BOM 同步向后兼容 Assembler 测试 (CR-004)
 */
@DisplayName("BOM Assembler 向后兼容")
class BomAssemblerTest {

    @Test
    @DisplayName("BomSoftwareBuildVersionOapi -> Po: softwareDesc 映射到 changeNote")
    void toPo_mapsSoftwareDescToChangeNote() {
        BomSoftwareBuildVersionOapi oapi = BomSoftwareBuildVersionOapi.builder()
                .deviceCode("TBOX")
                .softwarePn("S00000001AA")
                .softwareBuildVer("V1.0.0")
                .softwareDesc("修复蓝牙连接问题")
                .softwareSource("BOM")
                .releaseDate(new Date())
                .build();

        SoftwareBuildVersionPo po = BomSoftwareBuildVersionOapiAssembler.INSTANCE.toPo(oapi);

        assertEquals("修复蓝牙连接问题", po.getChangeNote());
    }

    @Test
    @DisplayName("BomSoftwareBuildVersionOapi -> Po: buildState 默认 DRAFT")
    void toPo_defaultsBuildStateToDraft() {
        BomSoftwareBuildVersionOapi oapi = BomSoftwareBuildVersionOapi.builder()
                .deviceCode("TBOX")
                .softwarePn("S00000001AA")
                .softwareBuildVer("V1.0.0")
                .softwareSource("BOM")
                .build();

        SoftwareBuildVersionPo po = BomSoftwareBuildVersionOapiAssembler.INSTANCE.toPo(oapi);

        assertEquals(SoftwareBuildVersionState.DRAFT.name(), po.getBuildState());
    }

    @Test
    @DisplayName("Po -> BomSoftwareBuildVersionOapi: changeNote 映射到 softwareDesc")
    void fromPo_mapsChangeNoteToSoftwareDesc() {
        SoftwareBuildVersionPo po = new SoftwareBuildVersionPo();
        po.setDeviceCode("TBOX");
        po.setSoftwarePn("S00000001AA");
        po.setSoftwareBuildVer("V1.0.0");
        po.setChangeNote("修复蓝牙连接问题");
        po.setSoftwareSource("BOM");
        po.setBuildState(SoftwareBuildVersionState.RELEASED.name());

        BomSoftwareBuildVersionOapi oapi = BomSoftwareBuildVersionOapiAssembler.INSTANCE.fromPo(po);

        assertEquals("修复蓝牙连接问题", oapi.getSoftwareDesc());
    }

    @Test
    @DisplayName("BomSoftwarePackageOapi -> Po: packageState 默认 ACTIVE")
    void toPo_packageDefaultsToActive() {
        BomSoftwarePackageOapi oapi = BomSoftwarePackageOapi.builder()
                .deviceCode("TBOX")
                .softwarePn("S00000001AA")
                .packageName("TBOX固件包")
                .packageType(1)
                .packageSource(1)
                .build();

        SoftwarePackagePo po = BomSoftwarePackageOapiAssembler.INSTANCE.toPo(oapi);

        assertEquals(SoftwarePackageState.ACTIVE.name(), po.getPackageState());
    }
}
