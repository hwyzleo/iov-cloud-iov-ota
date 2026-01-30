package net.hwyz.iov.cloud.ota.pota.api.feign.service.factory;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordExService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExFixedConfigWordService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 固定配置字相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class ExFixedConfigWordServiceFallbackFactory implements FallbackFactory<ExFixedConfigWordService> {

    @Override
    public ExFixedConfigWordService create(Throwable throwable) {
        return new ExFixedConfigWordService() {
            @Override
            public TableDataInfo list(FixedConfigWordExService fixedConfigWord) {
                logger.error("固定配置字相关服务查询固定配置字列表调用异常", throwable);
                return null;
            }

            @Override
            public FixedConfigWordExService getInfo(Long fixedConfigWordId) {
                logger.error("固定配置字相关服务根据固定配置字ID[{}]获取固定配置字信息调用异常", fixedConfigWordId, throwable);
                return null;
            }
        };
    }
}
