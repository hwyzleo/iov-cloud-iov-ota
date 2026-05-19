package net.hwyz.iov.cloud.iov.ota.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.iov.ota.api.service.OtaCompatiblePnService;
import net.hwyz.iov.cloud.iov.ota.api.vo.CompatiblePnExService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 兼容零件号相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class OtaCompatiblePnServiceFallbackFactory implements FallbackFactory<OtaCompatiblePnService> {

    @Override
    public OtaCompatiblePnService create(Throwable throwable) {
        return new OtaCompatiblePnService() {
            @Override
            public PageResult<CompatiblePnExService> list(CompatiblePnExService compatiblePn) {
                log.error("兼容零件号相关服务查询兼容零件号列表调用异常", throwable);
                return null;
            }

            @Override
            public CompatiblePnExService getInfo(Long compatiblePnId) {
                log.error("兼容零件号相关服务根据兼容零件号ID[{}]获取兼容零件号信息调用异常", compatiblePnId, throwable);
                return null;
            }
        };
    }
}
