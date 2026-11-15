package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.Empty;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.common.v1.Envelope.VehicleMessageEnvelope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FOTA 入站事务边界测试（2026-11-15 实车问题回归）。
 *
 * <p>实车现象：安装许可被领域守卫拒绝（车端 {@code allowed=false} 语义已实现），但内层
 * {@code @Transactional} 业务方法抛 {@code ExecutionStateException} 把共享事务标记 rollback-only，
 * 提交阶段抛 {@code UnexpectedRollbackException}，拒绝响应连同 Outbox 一起回滚；Kafka 视为可恢复
 * 故障反复重投，最终退避耗尽落盘隔离，车端始终拿不到任何答复。
 *
 * <p>本测试锁定两条不变式：
 * <ol>
 *   <li>业务异常必须先让业务事务回滚，再用 {@code REQUIRES_NEW} 事务写拒绝响应 + Inbox 记账；</li>
 *   <li>业务方法吞掉异常后事务仍被标记 rollback-only 时，同样要转为可表达的错误响应，不得上抛。</li>
 * </ol>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FotaKafkaInboundHandler - 业务异常与事务边界隔离")
class FotaKafkaInboundTransactionTest {

    private static final String PAYLOAD_TYPE = "vehicle.fota.v1.InstallPermitRequest";

    @Mock private FotaEnvelopeValidator validator;
    @Mock private FotaPayloadRouter router;
    @Mock private KafkaInboxRepository inboxRepository;
    @Mock private FotaPayloadHandler<Empty> handler;

    private FakeTransactionManager txManager;
    private FotaKafkaInboundHandler inboundHandler;

    @BeforeEach
    void setUp() throws Exception {
        txManager = new FakeTransactionManager();
        inboundHandler = new FotaKafkaInboundHandler(validator, router, inboxRepository,
                new KafkaMessagingMetricsService(), txManager);
        org.mockito.Mockito.doReturn(handler).when(router).resolve(PAYLOAD_TYPE);        when(handler.payloadType()).thenReturn(PAYLOAD_TYPE);
        when(handler.messageKind()).thenReturn(MessageKind.MESSAGE_KIND_REQUEST);
        when(handler.parse(any())).thenReturn(Empty.getDefaultInstance());
        when(inboxRepository.selectForUpdate(any(), any())).thenReturn(null);
    }

    @Test
    @DisplayName("业务异常：业务事务回滚后，拒绝响应在独立事务落库并记账 Inbox")
    void business_exception_writes_failure_response_in_separate_transaction() {
        when(handler.handle(any(), any())).thenThrow(new IllegalStateException("车辆任务状态[DOWNLOAD_PENDING]不在就绪状态族"));
        when(handler.handleFailure(any(), any(), eq("BUSINESS_ERROR"), anyString())).thenReturn(77L);

        assertDoesNotThrow(() -> inboundHandler.processMessage(record()));

        verify(inboxRepository).markProcessed(org.mockito.ArgumentMatchers.argThat(
                po -> Long.valueOf(77L).equals(po.getResultMessageId())));
        assertEquals(1, txManager.rollbackCount(), "业务事务必须先回滚，避免脏写提交");
        assertEquals(1, txManager.commitCount(), "拒绝响应事务必须独立提交");
        assertTrue(txManager.usedRequiresNew(), "拒绝响应必须运行在 REQUIRES_NEW 新事务中");
    }

    @Test
    @DisplayName("业务方法吞异常但事务被标记 rollback-only：不回滚无限重投，转为错误响应")
    void rollback_only_business_transaction_falls_back_to_failure_response() {
        // 模拟内层 @Transactional 抛业务异常（拦截器已打 rollback-only），上层 catch 后照常返回拒绝响应
        when(handler.handle(any(), any())).thenAnswer(invocation -> {
            txManager.markSharedTransactionRollbackOnly();
            return 11L;
        });
        when(handler.handleFailure(any(), any(), eq("TRANSACTION_ROLLBACK_ONLY"), anyString())).thenReturn(88L);

        assertDoesNotThrow(() -> inboundHandler.processMessage(record()));

        verify(handler).handleFailure(any(), any(), eq("TRANSACTION_ROLLBACK_ONLY"), anyString());
        verify(inboxRepository).markProcessed(org.mockito.ArgumentMatchers.argThat(
                po -> Long.valueOf(88L).equals(po.getResultMessageId())));
    }

    @Test
    @DisplayName("handler 未提供拒绝响应：业务异常原样上抛，保持可恢复重投语义")
    void business_exception_without_failure_support_is_rethrown() {
        when(handler.handle(any(), any())).thenThrow(new IllegalStateException("数据库连接不可用"));
        // Mockito 对 Long 返回类型默认返回 0，需显式声明「不支持拒绝响应」（接口默认实现即返回 null）
        org.mockito.Mockito.doReturn(null).when(handler).handleFailure(any(), any(), any(), any());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> inboundHandler.processMessage(record()));

        assertEquals("数据库连接不可用", thrown.getMessage());
        verify(inboxRepository, never()).markProcessed(any());
    }

    // ------------------------------------------------------------------ helpers

    private static ConsumerRecord<String, byte[]> record() {
        VehicleMessageEnvelope envelope = VehicleMessageEnvelope.newBuilder()
                .setRequestId("req-1")
                .setTimestampMs(System.currentTimeMillis())
                .setProtocolVersion("fota-v1")
                .setDeviceId("dev-1")
                .setVin("HWYZTEST900000001")
                .setMessageId("msg-1")
                .setMessageKind(MessageKind.MESSAGE_KIND_REQUEST)
                .setService("vehicle.fota")
                .setPayloadType(PAYLOAD_TYPE)
                .setPayload(Empty.getDefaultInstance().toByteString())
                .build();
        return new ConsumerRecord<>("vagw.fota", 0, 2551L, envelope.getVin(), envelope.toByteArray());
    }
}
