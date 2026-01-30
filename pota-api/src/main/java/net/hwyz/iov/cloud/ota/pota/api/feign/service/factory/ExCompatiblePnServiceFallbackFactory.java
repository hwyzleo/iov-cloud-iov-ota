package net.hwyz.iov.cloud.ota.pota.api.feign.service.factory;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.CompatiblePnExService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExCompatiblePnService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 兼容零件号相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class ExCompatiblePnServiceFallbackFactory implements FallbackFactory<ExCompatiblePnService> {

    @Override
    public ExCompatiblePnService create(Throwable throwable) {
        return new ExCompatiblePnService() {
            @Override
            public TableDataInfo list(CompatiblePnExService compatiblePn) {
                logger.error("兼容零件号相关服务查询兼容零件号列表调用异常", throwable);
                return null;
            }

            @Override
            public CompatiblePnExService getInfo(Long compatiblePnId) {
                logger.error("兼容零件号相关服务根据兼容零件号ID[{}]获取兼容零件号信息调用异常", compatiblePnId, throwable);
                return null;
            }
        };
    }
}
