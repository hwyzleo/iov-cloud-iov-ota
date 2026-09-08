package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 车辆清单观测消息生产者测试（CR-019 §7/§6.4）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InventoryObservedOutboxProducer 观测消息（CR-019）")
class InventoryObservedOutboxProducerTest {

    @Mock private CloudEventOutboxRepository cloudEventOutboxRepository;
    @Mock private KafkaMessagingMetricsService metrics;

    @InjectMocks
    private InventoryObservedOutboxProducer producer;

    private CanonicalVehicleInventory inventory() {
        return CanonicalVehicleInventory.builder()
                .vin("LSVAU2188N2ZG4G")
                .inventoryRevision(7L)
                .collectedAt(Instant.parse("2026-09-08T00:00:00Z"))
                .canonicalizationVersion(2)
                .ecuList(List.of(CanonicalEcu.builder()
                        .ecuId("ECU1").hardwarePartNumber("HPN1").hwVersion("H1")
                        .softwareUnits(List.of(new CanonicalSoftwareUnit(
                                "ECU_IMAGE", "SPN1", "V1.0", null, true, null)))
                        .build()))
                .build();
    }

    @Test
    @DisplayName("生成观测事件：observation_key 唯一、事件字段完整")
    void produces_observation_event() {
        byte[] digest = new byte[32];
        when(cloudEventOutboxRepository.findByBusinessKey(any())).thenReturn(null);
        when(cloudEventOutboxRepository.append(any())).thenReturn(true);

        boolean created = producer.produce(inventory(), digest, "SINGLE_IMAGE",
                Instant.parse("2026-09-08T01:00:00Z"));

        assertTrue(created);
        ArgumentCaptor<CloudEventOutboxPo> captor = ArgumentCaptor.forClass(CloudEventOutboxPo.class);
        verify(cloudEventOutboxRepository).append(captor.capture());
        CloudEventOutboxPo po = captor.getValue();
        assertEquals(CloudEventOutboxPo.EVENT_INVENTORY_OBSERVED, po.getEventType());
        assertEquals("LSVAU2188N2ZG4G", po.getVin());
        assertTrue(po.getBusinessKey().startsWith("VEHICLE_INVENTORY_OBSERVED:"));
        assertTrue(po.getPayloadJson().contains("\"softwareTargetCode\":\"ECU_IMAGE\""));
        assertTrue(po.getPayloadJson().contains("\"inventoryRevision\":7"));
        assertTrue(po.getPayloadJson().contains("\"inventoryModel\":\"SINGLE_IMAGE\""));
    }

    @Test
    @DisplayName("相同观测身份不重复建 Outbox（幂等）")
    void duplicate_observation_key_no_new_outbox() {
        when(cloudEventOutboxRepository.findByBusinessKey(any())).thenReturn(CloudEventOutboxPo.builder().build());
        boolean created = producer.produce(inventory(), new byte[32], "SINGLE_IMAGE", Instant.now());
        assertFalse(created);
        verify(cloudEventOutboxRepository, never()).append(any());
    }

    @Test
    @DisplayName("observation_key 稳定：同输入同键")
    void observation_key_stable() {
        byte[] d1 = new byte[32];
        byte[] d2 = new byte[32];
        String k1 = InventoryObservedOutboxProducer.observationKey("VIN1", 7L, d1);
        String k2 = InventoryObservedOutboxProducer.observationKey("VIN1", 7L, d2);
        assertEquals(k1, k2);
        assertNotEquals(k1, InventoryObservedOutboxProducer.observationKey("VIN2", 7L, d1));
        assertNotEquals(k1, InventoryObservedOutboxProducer.observationKey("VIN1", 8L, d1));
    }
}
