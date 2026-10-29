package net.hwyz.iov.cloud.iov.ota.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.framework.web.jackson.IovEpochMillisWriteModule;
import net.hwyz.iov.cloud.framework.web.jackson.IovJavaTimeReadModule;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskApprovalMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleSoftwareInventoryObservedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组织自有 JSON java.time 时间契约 codec 单测（CR-021 §4.25.4）
 *
 * <p>按框架 IovJacksonAutoConfiguration 的真实装配方式构造 mapper：
 * IovJavaTimeReadModule（双读）始终注册 + 写模式由 date-format 决定。
 * 验证旧整数 golden 不变、ISO/旧整数双读、配置时区确定性及排除项不变量。</p>
 */
@DisplayName("JSON java.time 时间契约 codec（CR-021 §4.25.4）")
class IovJacksonTimeCodecTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Instant SAMPLE = Instant.parse("2026-07-17T08:00:00Z");
    private static final long SAMPLE_EPOCH_MILLIS = 1784275200000L;

    /** 兼容阶段：双读 + 旧毫秒写（date-format=epoch-millis） */
    private ObjectMapper epochMillisMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new IovJavaTimeReadModule(ZONE));
        mapper.registerModule(new IovEpochMillisWriteModule(ZONE));
        return mapper;
    }

    /** ISO 阶段：双读 + JavaTimeModule ISO 写（date-format=iso） */
    private ObjectMapper isoMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new IovJavaTimeReadModule(ZONE));
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Test
    @DisplayName("epoch-millis 模式：Instant 旧整数输出 golden 不变")
    void epochMillisWriteInstantGolden() throws Exception {
        ObjectMapper mapper = epochMillisMapper();
        assertEquals(String.valueOf(SAMPLE_EPOCH_MILLIS), mapper.writeValueAsString(SAMPLE));
    }

    @Test
    @DisplayName("epoch-millis 模式：LocalDateTime 按配置时区换算 epoch-millis")
    void epochMillisWriteLocalDateTimeUsesConfiguredZone() throws Exception {
        ObjectMapper mapper = epochMillisMapper();
        LocalDateTime wallClock = LocalDateTime.of(2026, 7, 17, 16, 0, 0); // Asia/Shanghai 墙上
        // 与配置时区 Asia/Shanghai 的换算一致，不随 JVM 默认时区漂移
        assertEquals(String.valueOf(wallClock.atZone(ZONE).toInstant().toEpochMilli()),
                mapper.writeValueAsString(wallClock));
    }

    @Test
    @DisplayName("双读：Instant 接受 ISO 串与旧整数，得到同一时刻")
    void dualReadInstantIsoAndNumber() throws Exception {
        ObjectMapper mapper = epochMillisMapper();
        Instant fromIso = mapper.readValue("\"2026-07-17T08:00:00Z\"", Instant.class);
        Instant fromNumber = mapper.readValue(String.valueOf(SAMPLE_EPOCH_MILLIS), Instant.class);
        assertEquals(SAMPLE, fromIso);
        assertEquals(SAMPLE, fromNumber);
    }

    @Test
    @DisplayName("双读：LocalDateTime 数字输入按配置时区解释（非 JVM 默认）")
    void dualReadLocalDateTimeNumberUsesConfiguredZone() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            ObjectMapper mapper = epochMillisMapper();
            LocalDateTime ldt = mapper.readValue(String.valueOf(SAMPLE_EPOCH_MILLIS), LocalDateTime.class);
            // JVM 为 UTC 时 systemDefault 会得到 08:00；配置时区 Asia/Shanghai 应得到 16:00
            assertEquals(LocalDateTime.of(2026, 7, 17, 16, 0, 0), ldt);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    @DisplayName("双读：OffsetDateTime 接受 ISO 串与数字")
    void dualReadOffsetDateTime() throws Exception {
        ObjectMapper mapper = epochMillisMapper();
        OffsetDateTime fromIso = mapper.readValue("\"2026-07-17T16:00:00+08:00\"", OffsetDateTime.class);
        OffsetDateTime fromNumber = mapper.readValue(String.valueOf(SAMPLE_EPOCH_MILLIS), OffsetDateTime.class);
        assertEquals(OffsetDateTime.of(2026, 7, 17, 16, 0, 0, 0, ZoneId.of("Asia/Shanghai").getRules()
                .getOffset(Instant.parse("2026-07-17T08:00:00Z"))), fromIso);
        assertEquals(fromIso.toInstant(), fromNumber.toInstant());
    }

    @Test
    @DisplayName("ISO 模式：Instant 输出带 Z 的 ISO 字符串（绝对时刻含 offset）")
    void isoModeWritesOffsetIsoString() throws Exception {
        ObjectMapper mapper = isoMapper();
        String json = mapper.writeValueAsString(SAMPLE);
        assertEquals("\"2026-07-17T08:00:00Z\"", json);
    }

    @Test
    @DisplayName("ISO 模式：LocalDateTime 输出无时区 ISO 串，不做 RFC3339 冒充")
    void isoModeWritesLocalDateTimeWithoutZone() throws Exception {
        ObjectMapper mapper = isoMapper();
        String json = mapper.writeValueAsString(LocalDateTime.of(2026, 7, 17, 16, 0, 0));
        assertEquals("\"2026-07-17T16:00:00\"", json);
    }

    @Test
    @DisplayName("兼容阶段 MVC 输出 golden：TaskApprovalMpt.decidedAt 保持旧整数 epoch-millis")
    void taskApprovalDecidedAtEpochMillisGolden() throws Exception {
        ObjectMapper mapper = epochMillisMapper();
        TaskApprovalMpt vo = TaskApprovalMpt.builder()
                .taskId(1L)
                .decidedAt(SAMPLE)
                .build();
        JsonNode node = mapper.readTree(mapper.writeValueAsString(vo));
        assertEquals(SAMPLE_EPOCH_MILLIS, node.get("decidedAt").asLong());
        assertTrue(node.get("decidedAt").isNumber(), "decidedAt 必须是数字（epoch-millis），不得是数组/字符串");
    }

    @Test
    @DisplayName("排除项：观测事件 collectedAt/acceptedAt 为冻结 String ISO，不受 mapper 切换影响")
    void inventoryObservedEventIsoStringsUnchanged() throws Exception {
        // 与 InventoryObservedOutboxProducer.toJson 相同的序列化方式（findAndAddModules 手工 JsonMapper）
        ObjectMapper producerMapper = JsonMapper.builder().findAndAddModules().build();
        VehicleSoftwareInventoryObservedEvent event = VehicleSoftwareInventoryObservedEvent.builder()
                .eventId("e1")
                .observationKey("k1")
                .vin("VIN123")
                .inventoryRevision(1L)
                .inventoryModel("SINGLE_IMAGE")
                .collectedAt("2026-07-17T08:00:00Z")
                .acceptedAt("2026-07-17T09:00:00Z")
                .canonicalizationVersion(2)
                .canonicalDigest("abc")
                .build();
        JsonNode node = producerMapper.readTree(producerMapper.writeValueAsString(event));
        assertEquals("2026-07-17T08:00:00Z", node.get("collectedAt").asText());
        assertEquals("2026-07-17T09:00:00Z", node.get("acceptedAt").asText());
    }
}
