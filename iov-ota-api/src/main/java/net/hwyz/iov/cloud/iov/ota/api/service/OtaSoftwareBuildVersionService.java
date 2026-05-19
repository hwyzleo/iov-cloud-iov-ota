package net.hwyz.iov.cloud.iov.ota.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.iov.ota.api.fallback.OtaSoftwareBuildVersionServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 软件内部版本相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "otaSoftwareBuildVersionService", value = ServiceNameConstants.IOV_OTA, path = "/api/service/softwareBuildVersion/v1", fallbackFactory = OtaSoftwareBuildVersionServiceFallbackFactory.class)
public interface OtaSoftwareBuildVersionService {

    /**
     * 根据软件内部版本信息ID获取软件内部版本信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     * @return 软件内部版本信息
     */
    @GetMapping(value = "/{softwareBuildVersionId}")
    SoftwareBuildVersionExService getInfo(@PathVariable Long softwareBuildVersionId);

    /**
     * 根据ECU代码、软件零件号、软件内部版本获取软件内部版本信息
     *
     * @param deviceCode       设备代码
     * @param softwarePn       软件零件号（包含软件零件版本）
     * @param softwareBuildVer 软件内部版本
     * @return 软件内部版本信息
     */
    @GetMapping(value = "/info")
    SoftwareBuildVersionExService getInfo(@RequestParam String deviceCode, @RequestParam String softwarePn, @RequestParam String softwareBuildVer);

}
