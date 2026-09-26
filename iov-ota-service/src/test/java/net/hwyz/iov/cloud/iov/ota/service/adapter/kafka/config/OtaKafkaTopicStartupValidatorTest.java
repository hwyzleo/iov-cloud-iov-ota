package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config;

import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config.OtaKafkaTopicStartupValidator.TopicDescribeResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaTopicStartupException;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OTA Kafka Topic 启动校验器测试（CR-020 §7 初始化/边界）
 *
 * <p>通过 Stub 覆写 describeTopics / fetchTopicConfigs，覆盖：
 * 消费 Topic 存在/缺失（fail-fast 与 warn）、生产 Topic 配置漂移（分区/副本/retention）、
 * 旧名称残留、Admin 错误分类与生产 Topic 缺失（初始化开关）等场景。
 *
 * @author hwyz_leo
 */
@DisplayName("OtaKafkaTopicStartupValidator 启动校验（CR-020）")
class OtaKafkaTopicStartupValidatorTest {

    // ---------- 测试数据 ----------

    private static TopicDescription topic(String name, int partitions, int replicas) {
        List<TopicPartitionInfo> infos = new ArrayList<>();
        for (int i = 0; i < partitions; i++) {
            List<Node> nodes = new ArrayList<>();
            for (int r = 0; r < replicas; r++) {
                nodes.add(new Node(r, "broker-" + r, 9092));
            }
            infos.add(new TopicPartitionInfo(i, nodes.get(0), nodes, nodes));
        }
        return new TopicDescription(name, false, infos);
    }

    /** 全部 7 个 Topic 存在且配置匹配（3 分区 / 1 副本） */
    private static void allPresent(StubValidator validator) {
        validator.described.put("vagw.fota", TopicDescribeResult.present(topic("vagw.fota", 3, 1)));
        validator.described.put("vagw.fota.dlq.up", TopicDescribeResult.present(topic("vagw.fota.dlq.up", 3, 1)));
        validator.described.put("ota.fota", TopicDescribeResult.present(topic("ota.fota", 3, 1)));
        validator.described.put("ota.fota.dlq.up", TopicDescribeResult.present(topic("ota.fota.dlq.up", 3, 1)));
        validator.described.put("ota.vehicle-software-inventory.observed",
                TopicDescribeResult.present(topic("ota.vehicle-software-inventory.observed", 3, 1)));
        validator.described.put("vmd.vehicle-produce",
                TopicDescribeResult.present(topic("vmd.vehicle-produce", 3, 1)));
        validator.described.put("vmd.vehcile-part-binding.changed",
                TopicDescribeResult.present(topic("vmd.vehcile-part-binding.changed", 3, 1)));
    }

    /** 三个生产 Topic 定义与默认值一致 */
    private static OtaKafkaTopicDefinitionProperties definitions() {
        OtaKafkaTopicDefinitionProperties definitions = new OtaKafkaTopicDefinitionProperties();
        definitions.setDefaultPartitions(3);
        definitions.setDefaultReplicationFactor((short) 1);
        return definitions;
    }

    // ---------- 测试用例 ----------

    @Test
    @DisplayName("消费/生产 Topic 全部存在且配置匹配：无违规")
    void all_topics_present_no_violations() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.FAIL_FAST);
        StubValidator validator = new StubValidator(new OtaKafkaTopicsProperties(), definitions(), startup);
        allPresent(validator);

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();

        assertTrue(report.violations().isEmpty(), report.describe());
        assertDoesNotThrow(() -> validator.actOnReport(report));
    }

    @Test
    @DisplayName("消费 Topic 缺失：fail-fast 阻断（绝不补建）")
    void consumer_topic_missing_fail_fast() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.FAIL_FAST);
        StubValidator validator = new StubValidator(new OtaKafkaTopicsProperties(), definitions(), startup);
        // vagw.fota 缺失（describe 缺省按 MISSING），其余存在
        validator.described.put("vagw.fota.dlq.up", TopicDescribeResult.present(topic("vagw.fota.dlq.up", 3, 1)));
        allPresent(validator);
        validator.described.remove("vagw.fota");

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();
        assertTrue(report.violations().stream()
                .anyMatch(v -> v.type() == OtaKafkaTopicStartupValidator.Violation.Type.CONSUMER_TOPIC_MISSING
                        && "vagw.fota".equals(v.topic())));
        assertThrows(OtaKafkaTopicStartupException.class, () -> validator.actOnReport(report));
    }

    @Test
    @DisplayName("消费 Topic 缺失：warn 模式不阻断")
    void consumer_topic_missing_warn() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.WARN);
        StubValidator validator = new StubValidator(new OtaKafkaTopicsProperties(), definitions(), startup);
        allPresent(validator);
        validator.described.remove("vagw.fota");

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();
        assertFalse(report.violations().isEmpty());
        assertDoesNotThrow(() -> validator.actOnReport(report));
    }

    @Test
    @DisplayName("生产 Topic 分区/副本漂移：CONFIG_DRIFT")
    void producer_config_drift_partitions_and_replicas() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.WARN);
        StubValidator validator = new StubValidator(new OtaKafkaTopicsProperties(), definitions(), startup);
        allPresent(validator);
        // ota.fota 实际 6 分区 / 3 副本
        validator.described.put("ota.fota", TopicDescribeResult.present(topic("ota.fota", 6, 3)));

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();
        List<OtaKafkaTopicStartupValidator.Violation> drift = report.violations().stream()
                .filter(v -> v.type() == OtaKafkaTopicStartupValidator.Violation.Type.CONFIG_DRIFT
                        && "ota.fota".equals(v.topic()))
                .toList();
        assertEquals(2, drift.size());
        assertTrue(drift.stream().anyMatch(v -> v.detail().contains("partitions")));
        assertTrue(drift.stream().anyMatch(v -> v.detail().contains("replicas")));
    }

    @Test
    @DisplayName("生产 Topic retention 配置漂移：CONFIG_DRIFT")
    void producer_config_drift_via_configs() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.WARN);
        OtaKafkaTopicDefinitionProperties definitions = definitions();
        OtaKafkaTopicDefinitionProperties.Definition fota = new OtaKafkaTopicDefinitionProperties.Definition();
        fota.setPartitions(3);
        fota.setReplicationFactor((short) 1);
        fota.setConfigs(Map.of("retention.ms", "604800000"));
        definitions.getTopics().put("ota.fota", fota);

        StubValidator validator = new StubValidator(new OtaKafkaTopicsProperties(), definitions, startup);
        allPresent(validator);
        validator.configs.put("ota.fota", Map.of("retention.ms", "3600000"));

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();
        assertTrue(report.violations().stream().anyMatch(v ->
                v.type() == OtaKafkaTopicStartupValidator.Violation.Type.CONFIG_DRIFT
                        && v.detail().contains("retention.ms")));
    }

    @Test
    @DisplayName("旧名称残留：LEGACY_RESIDUE")
    void legacy_residue_detected() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.WARN);
        OtaKafkaTopicsProperties topics = new OtaKafkaTopicsProperties();
        topics.setFotaDown("iov.vagw.down.fota"); // 旧名称

        StubValidator validator = new StubValidator(topics, definitions(), startup);
        allPresent(validator);

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();
        assertTrue(report.violations().stream().anyMatch(v ->
                v.type() == OtaKafkaTopicStartupValidator.Violation.Type.LEGACY_RESIDUE));
    }

    @Test
    @DisplayName("Admin describe 鉴权/连接/超时错误分类为 ADMIN_ERROR")
    void describe_error_classified() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.WARN);
        StubValidator validator = new StubValidator(new OtaKafkaTopicsProperties(), definitions(), startup);
        validator.described.put("vagw.fota", TopicDescribeResult.error(
                OtaKafkaTopicStartupValidator.ErrorCategory.AUTH, "Authentication failed"));
        validator.described.put("vagw.fota.dlq.up", TopicDescribeResult.error(
                OtaKafkaTopicStartupValidator.ErrorCategory.TIMEOUT, "describe 超时"));
        validator.described.put("ota.fota", TopicDescribeResult.error(
                OtaKafkaTopicStartupValidator.ErrorCategory.CONNECTION, "broker unreachable"));
        validator.described.put("ota.fota.dlq.up", TopicDescribeResult.present(topic("ota.fota.dlq.up", 3, 1)));
        validator.described.put("ota.vehicle-software-inventory.observed",
                TopicDescribeResult.present(topic("ota.vehicle-software-inventory.observed", 3, 1)));

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();
        List<OtaKafkaTopicStartupValidator.Violation> adminErrors = report.violations().stream()
                .filter(v -> v.type() == OtaKafkaTopicStartupValidator.Violation.Type.ADMIN_ERROR)
                .toList();
        assertEquals(3, adminErrors.size());
    }

    @Test
    @DisplayName("生产 Topic 缺失且初始化开启：仅 note 不违规")
    void producer_missing_with_init_enabled_is_note() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.FAIL_FAST);
        startup.setInitializeProducerTopics(true);
        StubValidator validator = new StubValidator(new OtaKafkaTopicsProperties(), definitions(), startup);
        allPresent(validator);
        validator.described.remove("ota.fota");

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();
        assertTrue(report.violations().isEmpty(), report.describe());
        assertFalse(report.notes().isEmpty());
    }

    @Test
    @DisplayName("生产 Topic 缺失且初始化关闭：PRODUCER_TOPIC_MISSING")
    void producer_missing_with_init_disabled_is_violation() {
        OtaKafkaStartupProperties startup = new OtaKafkaStartupProperties();
        startup.setValidationMode(OtaKafkaStartupProperties.StartupValidationMode.WARN);
        startup.setInitializeProducerTopics(false);
        StubValidator validator = new StubValidator(new OtaKafkaTopicsProperties(), definitions(), startup);
        allPresent(validator);
        validator.described.remove("ota.fota.dlq.up");

        OtaKafkaTopicStartupValidator.ValidationReport report = validator.validate();
        assertTrue(report.violations().stream().anyMatch(v ->
                v.type() == OtaKafkaTopicStartupValidator.Violation.Type.PRODUCER_TOPIC_MISSING));
    }

    // ---------- Stub ----------

    static class StubValidator extends OtaKafkaTopicStartupValidator {

        final Map<String, TopicDescribeResult> described = new LinkedHashMap<>();
        final Map<String, Map<String, String>> configs = new LinkedHashMap<>();

        StubValidator(OtaKafkaTopicsProperties topics,
                      OtaKafkaTopicDefinitionProperties definitions,
                      OtaKafkaStartupProperties startup) {
            super(null, topics, definitions, startup);
        }

        @Override
        protected Map<String, TopicDescribeResult> describeTopics(Set<String> names) {
            Map<String, TopicDescribeResult> out = new LinkedHashMap<>();
            for (String name : names) {
                out.put(name, described.getOrDefault(name, TopicDescribeResult.missing()));
            }
            return out;
        }

        @Override
        protected Map<String, String> fetchTopicConfigs(String name) {
            return configs.getOrDefault(name, Map.of());
        }
    }
}
