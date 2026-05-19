package net.hwyz.iov.cloud.iov.ota.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.iov.ota.api.service.OtaSoftwareBuildVersionService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 软件零件版本相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class OtaSoftwareBuildVersionServiceFallbackFactory implements FallbackFactory<OtaSoftwareBuildVersionService> {

    @Override
    public OtaSoftwareBuildVersionService create(Throwable throwable) {
        return new OtaSoftwareBuildVersionService() {
            @Override
            public SoftwareBuildVersionExService getInfo(Long softwareBuildVersionId) {
                log.error("软件内部版本相关服务根据软件内部版本信息ID[{}]获取软件内部版本信息调用异常", softwareBuildVersionId, throwable);
                return null;
            }

            @Override
            public SoftwareBuildVersionExService getInfo(String deviceCode, String softwarePn, String softwareBuildVer) {
                log.error("软件内部版本相关服务根据软件设备代码[{}]、软件零件号[{}]、软件内部版本[{}]获取软件内部版本信息调用异常",
                        deviceCode, softwarePn, softwareBuildVer, throwable);
                return null;
            }
        };
    }
}
