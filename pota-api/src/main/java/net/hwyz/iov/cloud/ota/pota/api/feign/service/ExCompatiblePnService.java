package net.hwyz.iov.cloud.ota.pota.api.feign.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.CompatiblePnExService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.factory.ExCompatiblePnServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 兼容零件号相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exCompatiblePnService", value = ServiceNameConstants.OTA_BASELINE, path = "/service/compatiblePn", fallbackFactory = ExCompatiblePnServiceFallbackFactory.class)
public interface ExCompatiblePnService {

    /**
     * 分页查询兼容零件号列表
     *
     * @param compatiblePn 兼容零件号
     * @return 兼容零件号列表
     */
    @GetMapping(value = "/list")
    TableDataInfo list(CompatiblePnExService compatiblePn);

    /**
     * 根据兼容零件号ID获取兼容零件号信息
     *
     * @param compatiblePnId 兼容零件号ID
     * @return 兼容零件号信息
     */
    @GetMapping(value = "/{compatiblePnId}")
    CompatiblePnExService getInfo(@PathVariable Long compatiblePnId);

}
