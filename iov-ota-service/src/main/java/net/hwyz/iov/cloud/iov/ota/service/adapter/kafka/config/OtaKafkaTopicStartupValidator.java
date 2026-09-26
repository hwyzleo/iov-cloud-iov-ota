package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaTopicStartupException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * OTA Kafka Topic 启动校验器（CR-020 §4.24.2 / §4.24.3 / RD-020-3）
 *
 * <p>应用就绪时执行：
 * <ol>
 *   <li>旧名称残留校验：解析后的 Topic 映射与生产定义不得出现
 *       iov.vagw.up.fota / iov.vagw.down.fota / iov.vagw.delivery.fota 等目录外旧名称；</li>
 *   <li>消费 Topic 存在性断言：vagw.fota / vagw.fota.dlq.up / vmd.vehicle-produce /
 *       vmd.vehcile-part-binding.changed 必须已存在（由 VAGW/VMD 等上游治理），
 *       缺失时按校验模式 fail-fast 阻断启动，绝不补建；</li>
 *   <li>生产 Topic 配置校验：已存在的生产 Topic 与定义比对 partitions / replicas /
 *       关键 config，配置漂移按校验模式处理；缺失的由 {@link OtaProducerTopicInitializer}
 *       幂等创建，不在此创建。</li>
 * </ol>
 *
 * <p>Admin describe 错误分类：连接失败、鉴权失败、Topic 缺失、配置漂移、超时；
 * 日志不输出完整 VIN 或 payload。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class OtaKafkaTopicStartupValidator implements ApplicationListener<ApplicationReadyEvent> {

    /** describe/config 读取超时（毫秒） */
    static final long DESCRIBE_TIMEOUT_MS = 10_000L;

    /**
     * 目录外/旧名称残留检测名单（CR-020 §6.5 / §7 静态扫描约束的运行时对应）。
     * 历史文档引用除外；运行配置与解析结果中不允许出现。
     */
    public static final List<String> FORBIDDEN_LEGACY_TOPICS = List.of(
            "iov.vagw.up.fota",
            "iov.vagw.down.fota",
            "iov.vagw.delivery.fota",
            "iov.vagw.up.fota.dlq",
            "ota.vehicle-software-inventory.observed.dlq");

    private final Admin admin;
    private final OtaKafkaTopicsProperties topics;
    private final OtaKafkaTopicDefinitionProperties definitions;
    private final OtaKafkaStartupProperties startup;

    public OtaKafkaTopicStartupValidator(Admin admin,
                                         OtaKafkaTopicsProperties topics,
                                         OtaKafkaTopicDefinitionProperties definitions,
                                         OtaKafkaStartupProperties startup) {
        this.admin = admin;
        this.topics = topics;
        this.definitions = definitions;
        this.startup = startup;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        actOnReport(validate());
    }

    /**
     * 依据校验报告按启动策略处理（包内可见，便于测试）。
     */
    void actOnReport(ValidationReport report) {
        if (report.violations().isEmpty()) {
            log.info("Kafka Topic 启动校验通过：consumers={}，producers={}",
                    report.consumerTopics(), report.producerTopics());
            return;
        }
        if (startup.getValidationMode() == OtaKafkaStartupProperties.StartupValidationMode.FAIL_FAST) {
            log.error("Kafka Topic 启动校验失败（fail-fast 阻断启动）：{}", report.describe());
            throw new OtaKafkaTopicStartupException("Kafka Topic 启动校验失败: " + report.describe());
        }
        log.error("Kafka Topic 启动校验存在告警（warn 模式继续启动）：{}", report.describe());
    }

    /**
     * 执行完整启动校验（可独立测试）。
     */
    public ValidationReport validate() {
        ValidationReport.Builder builder = ValidationReport.builder()
                .consumerTopics(List.of(topics.getFotaUp(), topics.getVagwUpDlq(),
                        topics.getVmdVehicleProduce(), topics.getVmdPartBindingChanged()))
                .producerTopics(List.of(topics.getFotaDown(), topics.getFotaUpDlq(),
                        topics.getInventoryObserved()));

        detectLegacyResidue(builder);
        validateConsumerTopics(builder);
        validateProducerTopics(builder);
        return builder.build();
    }

    /**
     * 1. 旧名称/目录外名称残留校验（纯配置检查，不依赖 Broker）。
     */
    private void detectLegacyResidue(ValidationReport.Builder builder) {
        Set<String> resolved = new LinkedHashSet<>();
        resolved.add(builder.consumerTopics().get(0));
        resolved.add(builder.consumerTopics().get(1));
        resolved.addAll(builder.producerTopics());
        resolved.addAll(definitions.getTopics().keySet());

        for (String name : resolved) {
            if (name == null || name.isBlank()) {
                builder.violation(Violation.of(Violation.Type.CONFIG_INVALID, "<blank>",
                        "Topic 配置为空"));
                continue;
            }
            for (String legacy : FORBIDDEN_LEGACY_TOPICS) {
                if (legacy.equals(name)) {
                    builder.violation(Violation.of(Violation.Type.LEGACY_RESIDUE, name,
                            "目录外/旧名称残留，须切换至 Kafka Topic 目录基线名称"));
                }
            }
        }
    }

    /**
     * 2. 消费 Topic 存在性断言：缺失 fail-fast（绝不创建）。
     */
    private void validateConsumerTopics(ValidationReport.Builder builder) {
        Map<String, TopicDescribeResult> described =
                describeTopics(new LinkedHashSet<>(builder.consumerTopics()));
        for (Map.Entry<String, TopicDescribeResult> entry : described.entrySet()) {
            TopicDescribeResult result = entry.getValue();
            switch (result.kind()) {
                case PRESENT -> { /* 存在即通过 */ }
                case MISSING -> builder.violation(Violation.of(
                        Violation.Type.CONSUMER_TOPIC_MISSING, entry.getKey(),
                        "消费 Topic 缺失（fail-fast，绝不补建；由 VAGW/上游按目录创建）"));
                case ERROR -> builder.violation(Violation.of(
                        Violation.Type.ADMIN_ERROR, entry.getKey(),
                        result.category().name() + ": " + result.message()));
            }
        }
    }

    /**
     * 3. 生产 Topic 配置校验：已存在则比对配置漂移；缺失交由初始化器幂等创建。
     */
    private void validateProducerTopics(ValidationReport.Builder builder) {
        Map<String, TopicDescribeResult> described =
                describeTopics(new LinkedHashSet<>(builder.producerTopics()));
        for (Map.Entry<String, TopicDescribeResult> entry : described.entrySet()) {
            String name = entry.getKey();
            TopicDescribeResult result = entry.getValue();
            switch (result.kind()) {
                case ERROR -> builder.violation(Violation.of(
                        Violation.Type.ADMIN_ERROR, name,
                        result.category().name() + ": " + result.message()));
                case MISSING -> {
                    if (!startup.isInitializeProducerTopics()) {
                        builder.violation(Violation.of(Violation.Type.PRODUCER_TOPIC_MISSING, name,
                                "生产 Topic 缺失且初始化已关闭"));
                    } else {
                        builder.note("生产 Topic 缺失，交由 OtaProducerTopicInitializer 幂等创建: " + name);
                    }
                }
                case PRESENT -> validateProducerDrift(builder, name, result.description());
            }
        }
    }

    /**
     * 生产 Topic 配置漂移校验：partitions / replicas / 关键 config。
     */
    private void validateProducerDrift(ValidationReport.Builder builder, String name,
                                       TopicDescription description) {
        OtaKafkaTopicDefinitionProperties.Definition definition = definitions.getTopics().get(name);
        int expectedPartitions = definition != null && definition.getPartitions() != null
                ? definition.getPartitions() : definitions.getDefaultPartitions();
        short expectedReplicas = definition != null && definition.getReplicationFactor() != null
                ? definition.getReplicationFactor() : definitions.getDefaultReplicationFactor();

        int actualPartitions = description.partitions().size();
        int actualReplicas = description.partitions().isEmpty()
                ? 0 : description.partitions().get(0).replicas().size();

        if (actualPartitions != expectedPartitions) {
            builder.violation(Violation.of(Violation.Type.CONFIG_DRIFT, name,
                    "partitions 漂移：期望[" + expectedPartitions + "] 实际[" + actualPartitions + "]"));
        }
        if (actualReplicas != expectedReplicas) {
            builder.violation(Violation.of(Violation.Type.CONFIG_DRIFT, name,
                    "replicas 漂移：期望[" + expectedReplicas + "] 实际[" + actualReplicas + "]"));
        }

        Map<String, String> expectedConfigs = definition != null && definition.getConfigs() != null
                ? definition.getConfigs() : Map.of();
        if (!expectedConfigs.isEmpty()) {
            Map<String, String> actualConfigs = fetchTopicConfigs(name);
            for (Map.Entry<String, String> expected : expectedConfigs.entrySet()) {
                String actual = actualConfigs.get(expected.getKey());
                if (actual != null && !actual.equals(expected.getValue())) {
                    builder.violation(Violation.of(Violation.Type.CONFIG_DRIFT, name,
                            expected.getKey() + " 漂移：期望[" + expected.getValue() + "] 实际[" + actual + "]"));
                }
            }
        }
    }

    /**
     * 批量 describe（生产代码走 Kafka Admin；测试可覆写）。
     *
     * @return name → describe 结果（PRESENT / MISSING / ERROR）
     */
    protected Map<String, TopicDescribeResult> describeTopics(Set<String> names) {
        Map<String, TopicDescribeResult> result = new LinkedHashMap<>();
        DescribeTopicsResult describeResult;
        try {
            describeResult = admin.describeTopics(names);
        } catch (Exception e) {
            for (String name : names) {
                result.put(name, TopicDescribeResult.error(ErrorCategory.CONNECTION,
                        classify(e) + ": " + safeMessage(e)));
            }
            return result;
        }
        Map<String, KafkaFuture<TopicDescription>> futures = describeResult.topicNameValues();
        for (String name : names) {
            KafkaFuture<TopicDescription> future = futures.get(name);
            if (future == null) {
                result.put(name, TopicDescribeResult.error(ErrorCategory.UNKNOWN,
                        "describe 未返回该 Topic 的 future"));
                continue;
            }
            try {
                result.put(name, TopicDescribeResult.present(future.get(DESCRIBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)));
            } catch (TimeoutException e) {
                result.put(name, TopicDescribeResult.error(ErrorCategory.TIMEOUT,
                        "describe 超时(" + DESCRIBE_TIMEOUT_MS + "ms)"));
            } catch (ExecutionException e) {
                result.put(name, classifyDescribeFailure(e.getCause()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result.put(name, TopicDescribeResult.error(ErrorCategory.CONNECTION, "interrupted"));
            }
        }
        return result;
    }

    /**
     * 读取单 Topic 关键配置（生产代码走 Kafka Admin；测试可覆写）。
     * 失败时降级为不参与 config 漂移判定（partitions/replicas 漂移仍生效）。
     */
    protected Map<String, String> fetchTopicConfigs(String name) {
        try {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
            Config config = admin.describeConfigs(Collections.singleton(resource))
                    .all()
                    .get(DESCRIBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .get(resource);
            Map<String, String> result = new HashMap<>();
            for (ConfigEntry entry : config.entries()) {
                result.put(entry.name(), entry.value());
            }
            return result;
        } catch (Exception e) {
            log.warn("读取 Topic 配置失败，跳过关键配置漂移检查：topic={}，error={}",
                    name, safeMessage(e));
            return Map.of();
        }
    }

    private TopicDescribeResult classifyDescribeFailure(Throwable cause) {
        if (cause instanceof UnknownTopicOrPartitionException) {
            return TopicDescribeResult.missing();
        }
        if (cause instanceof AuthenticationException || cause instanceof AuthorizationException) {
            return TopicDescribeResult.error(ErrorCategory.AUTH,
                    cause.getClass().getSimpleName() + ": " + safeMessage(cause));
        }
        return TopicDescribeResult.error(ErrorCategory.CONNECTION,
                cause.getClass().getSimpleName() + ": " + safeMessage(cause));
    }

    private String classify(Throwable e) {
        if (e instanceof AuthenticationException || e instanceof AuthorizationException) {
            return ErrorCategory.AUTH.name();
        }
        if (e instanceof TimeoutException || e instanceof java.net.SocketTimeoutException
                || e instanceof java.util.concurrent.TimeoutException) {
            return ErrorCategory.TIMEOUT.name();
        }
        return ErrorCategory.CONNECTION.name();
    }

    private static String safeMessage(Throwable e) {
        return e == null || e.getMessage() == null ? e == null ? "null" : e.getClass().getSimpleName() : e.getMessage();
    }

    /**
     * describe 错误分类（连接 / 鉴权 / 超时 / 未知）。
     */
    public enum ErrorCategory {
        CONNECTION, AUTH, TIMEOUT, UNKNOWN
    }

    /**
     * 单个 Topic describe 结果。
     */
    public record TopicDescribeResult(Kind kind, TopicDescription description,
                                      ErrorCategory category, String message) {

        public enum Kind { PRESENT, MISSING, ERROR }

        public static TopicDescribeResult present(TopicDescription description) {
            return new TopicDescribeResult(Kind.PRESENT, description, null, null);
        }

        public static TopicDescribeResult missing() {
            return new TopicDescribeResult(Kind.MISSING, null, null, null);
        }

        public static TopicDescribeResult error(ErrorCategory category, String message) {
            return new TopicDescribeResult(Kind.ERROR, null, category, message);
        }
    }

    /**
     * 启动校验违规项。
     */
    public record Violation(Type type, String topic, String detail) {

        public enum Type {
            /** 消费 Topic 缺失（绝不补建） */
            CONSUMER_TOPIC_MISSING,
            /** 生产 Topic 缺失且初始化关闭 */
            PRODUCER_TOPIC_MISSING,
            /** 已存在生产 Topic 配置漂移 */
            CONFIG_DRIFT,
            /** 目录外/旧名称残留 */
            LEGACY_RESIDUE,
            /** Admin describe 连接/鉴权/超时等错误 */
            ADMIN_ERROR,
            /** 配置非法（空等） */
            CONFIG_INVALID
        }

        public static Violation of(Type type, String topic, String detail) {
            return new Violation(type, topic, detail);
        }
    }

    /**
     * 校验报告（不可变）。
     */
    public static class ValidationReport {

        private final List<String> consumerTopics;
        private final List<String> producerTopics;
        private final List<Violation> violations;
        private final List<String> notes;

        ValidationReport(List<String> consumerTopics, List<String> producerTopics,
                         List<Violation> violations, List<String> notes) {
            this.consumerTopics = List.copyOf(consumerTopics);
            this.producerTopics = List.copyOf(producerTopics);
            this.violations = List.copyOf(violations);
            this.notes = List.copyOf(notes);
        }

        public static Builder builder() {
            return new Builder();
        }

        public List<String> consumerTopics() {
            return consumerTopics;
        }

        public List<String> producerTopics() {
            return producerTopics;
        }

        public List<Violation> violations() {
            return violations;
        }

        public List<String> notes() {
            return notes;
        }

        public String describe() {
            StringBuilder sb = new StringBuilder();
            for (Violation v : violations) {
                sb.append('[').append(v.type()).append("] topic=").append(v.topic())
                        .append(" detail=").append(v.detail()).append("; ");
            }
            return sb.toString().trim();
        }

        public static class Builder {

            private final List<String> consumerTopics = new ArrayList<>();
            private final List<String> producerTopics = new ArrayList<>();
            private final List<Violation> violations = new ArrayList<>();
            private final List<String> notes = new ArrayList<>();

            public Builder consumerTopics(List<String> names) {
                consumerTopics.addAll(names);
                return this;
            }

            public Builder producerTopics(List<String> names) {
                producerTopics.addAll(names);
                return this;
            }

            public Builder violation(Violation violation) {
                violations.add(violation);
                return this;
            }

            public Builder note(String note) {
                notes.add(note);
                return this;
            }

            public List<String> consumerTopics() {
                return consumerTopics;
            }

            public List<String> producerTopics() {
                return producerTopics;
            }

            public ValidationReport build() {
                return new ValidationReport(consumerTopics, producerTopics, violations, notes);
            }
        }
    }
}
