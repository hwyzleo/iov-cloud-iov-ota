package net.hwyz.iov.cloud.iov.ota.api.service;

import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.ota.api.vo.CompatiblePnExService;
import net.hwyz.iov.cloud.iov.ota.api.fallback.OtaCompatiblePnServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 兼容零件号相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "otaCompatiblePnService", value = ServiceNameConstants.IOV_OTA, path = "/api/service/compatiblePn/v1", fallbackFactory = OtaCompatiblePnServiceFallbackFactory.class)
public interface OtaCompatiblePnService {

    /**
     * 分页查询兼容零件号列表
     *
     * @param compatiblePn 兼容零件号
     * @return 兼容零件号列表
     */
    @GetMapping(value = "/list")
    PageResult<CompatiblePnExService> list(CompatiblePnExService compatiblePn);

    /**
     * 根据兼容零件号ID获取兼容零件号信息
     *
     * @param compatiblePnId 兼容零件号ID
     * @return 兼容零件号信息
     */
    @GetMapping(value = "/{compatiblePnId}")
    CompatiblePnExService getInfo(@PathVariable Long compatiblePnId);

}
