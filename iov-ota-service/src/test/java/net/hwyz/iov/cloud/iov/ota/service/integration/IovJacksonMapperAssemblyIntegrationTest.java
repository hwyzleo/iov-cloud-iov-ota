package net.hwyz.iov.cloud.iov.ota.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.framework.web.autoconfigure.IovJacksonAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 框架 JSON 时间契约真实装配验证（CR-021 §4.25.2/4.25.4）
 *
 * <p>使用 Spring 真实装配（Boot JacksonAutoConfiguration + 框架
 * IovJacksonAutoConfiguration），验证：customizer 生效、启动探针通过、
 * 最终 mapper 的写模式与配置一致、双读可用、配置时区确定性、
 * 未知 date-format 值启动失败。加载等效共享配置，不临时构造理想 mapper。</p>
 */
@DisplayName("框架 JSON 时间契约真实装配（CR-021 §4.25.2/4.25.4）")
class IovJacksonMapperAssemblyIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    IovJacksonAutoConfiguration.class))
            .withPropertyValues(
                    "iov.web.jackson.date-format=epoch-millis",
                    "iov.web.jackson.zone-id=Asia/Shanghai");

    @Test
    @DisplayName("兼容阶段装配：上下文启动成功（启动探针通过），最终 mapper 可用")
    void contextStartsWithEpochMillisAndProbePasses() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ObjectMapper.class);
            assertThat(context).hasBean("iovWebJacksonStartupProbe");
        });
    }

    @Test
    @DisplayName("最终 mapper：写 epoch-millis 旧整数 golden 不变，ISO/旧整数双读")
    void finalMapperWritesEpochMillisAndReadsDual() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            // 写：旧整数 epoch-millis golden 不变
            String written = mapper.writeValueAsString(Instant.parse("2026-07-17T08:00:00Z"));
            assertThat(written).isEqualTo("1784275200000");
            // 读：ISO 与旧整数双读，得到同一时刻
            assertThat(mapper.readValue("\"2026-07-17T08:00:00Z\"", Instant.class))
                    .isEqualTo(Instant.parse("2026-07-17T08:00:00Z"));
            assertThat(mapper.readValue("1784275200000", Instant.class))
                    .isEqualTo(Instant.parse("2026-07-17T08:00:00Z"));
        });
    }

    @Test
    @DisplayName("配置时区确定性：LocalDateTime 数字输入按配置 zone-id 解释，不随 JVM 默认时区漂移")
    void localDateTimeUsesConfiguredZoneNotJvmDefault() {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            runner.run(context -> {
                ObjectMapper mapper = context.getBean(ObjectMapper.class);
                // 1784275200000 = 2026-07-17T08:00:00Z；Asia/Shanghai 墙上为 16:00
                LocalDateTime ldt = mapper.readValue("1784275200000", LocalDateTime.class);
                assertThat(ldt).isEqualTo(LocalDateTime.of(2026, 7, 17, 16, 0, 0));
            });
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    @DisplayName("写模式切换：date-format=iso 时最终 mapper 输出 ISO 字符串")
    void isoWriteModeOutputsIsoStrings() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        IovJacksonAutoConfiguration.class))
                .withPropertyValues(
                        "iov.web.jackson.date-format=iso",
                        "iov.web.jackson.zone-id=Asia/Shanghai")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    String written = mapper.writeValueAsString(Instant.parse("2026-07-17T08:00:00Z"));
                    assertThat(written).isEqualTo("\"2026-07-17T08:00:00Z\"");
                });
    }

    @Test
    @DisplayName("未知 date-format 值：启动失败（fail-fast，不静默回退）")
    void invalidDateFormatFailsStartup() {
        runner.withPropertyValues("iov.web.jackson.date-format=bogus")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("zone-id 缺失：启动失败（设计要求显式配置，不推定默认时区）")
    void missingZoneIdFailsStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        IovJacksonAutoConfiguration.class))
                .withPropertyValues("iov.web.jackson.date-format=epoch-millis")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("zone-id 显式合法值：探针通过，最终 mapper 的时区与配置一致")
    void explicitZoneIdHonoredByFinalMapper() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            // 最终 mapper 的 LocalDateTime 数字读按 Asia/Shanghai 解释（与配置一致）
            LocalDateTime ldt = mapper.readValue("1784275200000", LocalDateTime.class);
            assertThat(ldt).isEqualTo(LocalDateTime.of(2026, 7, 17, 16, 0, 0));
            assertThat(ldt.atZone(ZoneId.of("Asia/Shanghai")).toInstant())
                    .isEqualTo(Instant.parse("2026-07-17T08:00:00Z"));
        });
    }
}
