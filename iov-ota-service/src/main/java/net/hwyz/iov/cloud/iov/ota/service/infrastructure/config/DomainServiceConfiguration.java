package net.hwyz.iov.cloud.iov.ota.service.infrastructure.config;

import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwareBuildVersionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.CompatiblePnDomainService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.SoftwareBuildVersionDomainService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.SoftwarePackageDomainService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.VehiclePartDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain Service配置类
 * 用于注入Domain Service（Domain层不使用Spring注解）
 */
@Configuration
public class DomainServiceConfiguration {

    @Bean
    public SoftwareBuildVersionDomainService softwareBuildVersionDomainService(
            SoftwareBuildVersionRepository repository) {
        return new SoftwareBuildVersionDomainService(repository);
    }

    @Bean
    public SoftwarePackageDomainService softwarePackageDomainService(
            net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwarePackageRepository repository) {
        return new SoftwarePackageDomainService(repository);
    }

    @Bean
    public CompatiblePnDomainService compatiblePnDomainService(
            net.hwyz.iov.cloud.iov.ota.service.domain.repository.CompatiblePnRepository repository) {
        return new CompatiblePnDomainService(repository);
    }

    @Bean
    public VehiclePartDomainService vehiclePartDomainService(
            net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehiclePartRepository repository) {
        return new VehiclePartDomainService(repository);
    }
}