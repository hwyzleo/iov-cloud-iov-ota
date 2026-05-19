package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import lombok.Builder;
import lombok.Data;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwareBuildVersionDependency;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 软件内部版本聚合根
 */
@Data
@Builder
public class SoftwareBuildVersion implements Serializable {
    private SoftwareBuildVersionId id;
    private DeviceCode deviceCode;
    private SoftwarePn softwarePn;
    private String softwareBuildVer;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private String adaptiveAssemblyPn;
    private String adaptiveSoftwarePn;
    private Instant releaseDate;
    
    @Builder.Default
    private List<SoftwarePackage> packages = new ArrayList<>();
    
    @Builder.Default
    private List<SoftwareBuildVersionDependency> dependencies = new ArrayList<>();
}
