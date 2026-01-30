package net.hwyz.iov.cloud.ota.pota.api.feign.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordExService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.factory.ExFixedConfigWordServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 固定配置字相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exFixedConfigWordService", value = ServiceNameConstants.OTA_BASELINE, path = "/service/fixedConfigWord", fallbackFactory = ExFixedConfigWordServiceFallbackFactory.class)
public interface ExFixedConfigWordService {

    /**
     * 分页查询固定配置字列表
     *
     * @param fixedConfigWord 固定配置字
     * @return 固定配置字列表
     */
    @GetMapping(value = "/list")
    TableDataInfo list(FixedConfigWordExService fixedConfigWord);

    /**
     * 根据固定配置字ID获取固定配置字信息
     *
     * @param fixedConfigWordId 固定配置字ID
     * @return 固定配置字信息
     */
    @GetMapping(value = "/{fixedConfigWordId}")
    FixedConfigWordExService getInfo(@PathVariable Long fixedConfigWordId);

}
