package net.hwyz.iov.cloud.iov.ota.service.infrastructure.config;

import net.hwyz.iov.cloud.edd.oss.api.fallback.ImageServiceFallbackFactory;
import net.hwyz.iov.cloud.edd.oss.api.fallback.LogObjectServiceFallbackFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EDD-OSS Feign 客户端降级工厂注册（IOV-OTA-DSN-CR-026 §7.3）。
 *
 * <p>`edd-oss-api` 中的 {@code @Component} 工厂类位于本服务组件扫描范围之外；当平台开启
 * Feign circuit breaker 时，Spring Cloud 需要按类型解析 fallbackFactory Bean，缺失会导致
 * 启动失败。这里显式注册（关闭熔断时为无害的普通 Bean），并保持 fail-closed 语义：
 * 降级只会抛出可用性异常（403016），绝不伪造上传/核验成功。</p>
 *
 * @author hwyz_leo
 */
@Configuration
public class EddOssFeignConfig {

    /**
     * 日志对象客户端降级工厂。
     */
    @Bean
    public LogObjectServiceFallbackFactory logObjectServiceFallbackFactory() {
        return new LogObjectServiceFallbackFactory();
    }

    /**
     * 图片客户端降级工厂（随 edd-oss-api 一并被 Feign 扫描注册，保持同样可解析）。
     */
    @Bean
    public ImageServiceFallbackFactory imageServiceFallbackFactory() {
        return new ImageServiceFallbackFactory();
    }
}
