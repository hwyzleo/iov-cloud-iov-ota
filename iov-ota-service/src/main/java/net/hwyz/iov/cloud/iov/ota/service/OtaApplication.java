package net.hwyz.iov.cloud.iov.ota.service;


import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.annotation.EnableCustomConfig;
import net.hwyz.iov.cloud.framework.security.annotation.EnableCustomFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 *
 * @author hwyz_leo
 */
@Slf4j
@EnableCustomConfig
@EnableDiscoveryClient
@EnableCustomFeignClients
@EnableScheduling
@SpringBootApplication
public class OtaApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtaApplication.class, args);
        log.info("应用启动完成");
    }

}
