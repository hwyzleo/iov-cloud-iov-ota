package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import lombok.Builder;
import lombok.Data;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.PotaBaseException;
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
 * 软件内部版本聚合根（充血模型）
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

    /**
     * 检查是否与另一个版本重复
     * 业务规则：设备代码+软件零件号+版本号必须唯一
     */
    public boolean isDuplicateWith(SoftwareBuildVersion other) {
        if (other == null) return false;
        return this.deviceCode.equals(other.deviceCode)
            && this.softwarePn.equals(other.softwarePn)
            && this.softwareBuildVer.equals(other.softwareBuildVer)
            && !this.id.equals(other.id);
    }

    /**
     * 添加软件包
     * 业务规则：不能重复添加同一软件包
     */
    public void addPackage(SoftwarePackage pkg) {
        if (packages.stream().anyMatch(p -> p.getId().equals(pkg.getId()))) {
            throw new PotaBaseException("软件包已存在，不能重复添加");
        }
        packages.add(pkg);
    }

    /**
     * 批量添加软件包
     */
    public void addPackages(List<SoftwarePackage> newPackages) {
        newPackages.forEach(this::addPackage);
    }

    /**
     * 添加依赖
     * 业务规则：不能重复依赖同一版本
     */
    public void addDependency(SoftwareBuildVersionDependency dependency) {
        if (dependencies.stream().anyMatch(d -> 
            d.getDependencySoftwareBuildVersionId().equals(dependency.getDependencySoftwareBuildVersionId()))) {
            throw new PotaBaseException("依赖关系已存在，不能重复添加");
        }
        dependencies.add(dependency);
    }

    /**
     * 修改依赖的适配级别
     */
    public void modifyDependencyAdaptiveLevel(SoftwareBuildVersionId dependencyId, Integer adaptiveLevel) {
        dependencies.stream()
            .filter(d -> d.getDependencySoftwareBuildVersionId().equals(dependencyId))
            .findFirst()
            .orElseThrow(() -> new PotaBaseException("依赖关系不存在"))
            .setAdaptiveLevel(adaptiveLevel);
    }

    /**
     * 统计软件包数量
     */
    public int countPackages() {
        return packages.size();
    }

    /**
     * 统计依赖数量
     */
    public int countDependencies() {
        return dependencies.size();
    }
}
