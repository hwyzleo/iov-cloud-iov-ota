package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kafka 兜底本地隔离仓测试：原始 bytes 与元数据成对落盘，可人工回放。
 *
 * @author hwyz_leo
 */
@DisplayName("KafkaQuarantineStore 落盘隔离")
class KafkaQuarantineStoreTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("原始 value bytes 与元数据成对写入隔离目录")
    void stores_raw_bytes_and_metadata() throws Exception {
        byte[] raw = new byte[]{1, 2, 3, 4};
        ConsumerRecord<String, byte[]> record =
                new ConsumerRecord<>("vagw.fota", 2, 99L, "LSVAU2188N2ZG4G", raw);
        KafkaQuarantineStore store = new KafkaQuarantineStore(tempDir.toString());

        Path bodyFile = store.store(record, new IllegalStateException("db down"));

        assertTrue(Files.exists(bodyFile));
        assertArrayEquals(raw, Files.readAllBytes(bodyFile));

        Path metaFile = tempDir.resolve(bodyFile.getFileName().toString().replace(".bin", ".json"));
        assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        assertTrue(meta.contains("\"topic\":\"vagw.fota\""));
        assertTrue(meta.contains("\"partition\":2"));
        assertTrue(meta.contains("\"offset\":99"));
        // 回放需要原 Key（文件按敏感数据管理）
        assertTrue(meta.contains("LSVAU2188N2ZG4G"));
        assertTrue(meta.contains("db down"));
        // 不遗留临时文件
        assertEquals(2, Files.list(tempDir).count());
    }

    @Test
    @DisplayName("Topic 名中的非法文件名字符被替换")
    void sanitizes_topic_name() {
        ConsumerRecord<String, byte[]> record =
                new ConsumerRecord<>("a/b:c", 0, 1L, "K", new byte[]{7});
        Path bodyFile = new KafkaQuarantineStore(tempDir.toString())
                .store(record, new RuntimeException("x"));

        assertTrue(bodyFile.getFileName().toString().startsWith("q-"));
        assertTrue(bodyFile.getFileName().toString().contains("-a_b_c-"));
    }
}
