package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka 兜底本地隔离仓（Inbox 毒消息退避耗尽后的落盘隔离）。
 *
 * <p>当消息在 {@link org.springframework.kafka.listener.DefaultErrorHandler} 的有界
 * 退避重试后仍未成功（如 Kafka/DLQ 长时间不可用、数据库故障），必须让分区继续推进，
 * 否则一条毒消息会永久阻塞整个分区。本仓储把「原始 value bytes + 元数据」落盘为
 * 一对文件，保证消息不丢失且可人工回放：
 * <ul>
 *   <li>{@code <seq>-<topic>-p<partition>-o<offset>.bin}：value 原样字节；</li>
 *   <li>{@code ...json}：topic/partition/offset/key/reason 等回放所需元数据。</li>
 * </ul>
 * 文件先写临时文件再原子移动，避免半写文件被误回放。
 *
 * <p>安全：隔离目录含原始 Key(VIN) 与消息内容，属于敏感数据，须使用受限目录并在
 * 人工回放后清理；日志只输出脱敏 Key。
 *
 * @author hwyz_leo
 */
@Component
public class KafkaQuarantineStore {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path directory;

    public KafkaQuarantineStore(
            @Value("${ota.kafka.inbound.error-handler.quarantine-dir:./kafka-quarantine}")
            String directory) {
        this.directory = Paths.get(directory);
    }

    /**
     * 落盘一条无法处理的消息。
     *
     * @return 原始 bytes 文件路径
     */
    public Path store(ConsumerRecord<?, ?> record, Throwable error) {
        try {
            Files.createDirectories(directory);
            String base = "q-" + System.currentTimeMillis()
                    + "-" + sanitize(record.topic())
                    + "-p" + record.partition()
                    + "-o" + record.offset();
            Path bodyFile = directory.resolve(base + ".bin");
            Path metaFile = directory.resolve(base + ".json");

            byte[] body = record.value() instanceof byte[] bytes ? bytes : null;
            if (body != null) {
                writeAtomically(bodyFile, body);
            }
            writeAtomically(metaFile, objectMapper.writeValueAsBytes(metadata(record, error, bodyFile)));
            return bodyFile;
        } catch (IOException e) {
            throw new UncheckedIOException("写入 Kafka 隔离目录失败: " + directory, e);
        }
    }

    private Map<String, Object> metadata(ConsumerRecord<?, ?> record, Throwable error, Path bodyFile) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("quarantinedAt", Instant.now().toString());
        meta.put("topic", record.topic());
        meta.put("partition", record.partition());
        meta.put("offset", record.offset());
        // 回放必须使用原 Key：这里保留原文，文件本身按敏感数据管理。
        meta.put("key", record.key());
        meta.put("valueFile", bodyFile.getFileName().toString());
        meta.put("valueBytes", record.value() instanceof byte[] bytes ? bytes.length : 0);
        if (error != null) {
            meta.put("errorType", error.getClass().getName());
            meta.put("errorMessage", error.getMessage());
        }
        return meta;
    }

    private void writeAtomically(Path target, byte[] content) throws IOException {
        Path temp = directory.resolve(target.getFileName() + ".tmp");
        Files.write(temp, content);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private static String sanitize(String topic) {
        return topic == null ? "unknown" : topic.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
