package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * VMD 零件绑定事件消费者测试（CR-023 §6 / C1 并发唯一键）
 *
 * <p>回归点：消费者必须按 vin + pn 业务身份键定位命中行。
 * 早期实现使用无过滤条件的 {@code selectPoByMap}，单测与线上行为不一致：
 * 线上每次事件都会命中「全表 id 最大行」并改写它，导致新零件永不插入。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VMD 零件绑定消费者（CR-023）")
class VmdVehiclePartBindingKafkaConsumerTest {

    private static final String VIN = "LSVAU2188N2ZG4G";
    private static final String PN = "PN-1";
    private static final String NODE = "ECU_A";

    @Mock private VehiclePartMapper vehiclePartMapper;

    private VmdVehiclePartBindingKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new VmdVehiclePartBindingKafkaConsumer(vehiclePartMapper, new ObjectMapper());
    }

    private ConsumerRecord<String, String> record(String changeType) {
        String json = "{\"vin\":\"" + VIN + "\",\"partCode\":\"" + PN + "\","
                + "\"vehicleNodeCode\":\"" + NODE + "\",\"changeType\":\"" + changeType + "\"}";
        return new ConsumerRecord<>("vmd.vehicle-part-binding.changed", 0, 0L, "key", json);
    }

    @Test
    @DisplayName("BIND 新零件：按 vin+pn 查询后插入投影")
    void bind_inserts_new_part() {
        when(vehiclePartMapper.selectPoListByVinAndPn(anyString(), anyString())).thenReturn(List.of());

        consumer.onVehiclePartBindingChanged(record("BIND"));

        verify(vehiclePartMapper).selectPoListByVinAndPn(eq(VIN), eq(PN));
        ArgumentCaptor<VehiclePartPo> captor = ArgumentCaptor.forClass(VehiclePartPo.class);
        verify(vehiclePartMapper).insertPo(captor.capture());
        assertEquals(PN, captor.getValue().getPn());
        assertEquals(VIN, captor.getValue().getVin());
        assertEquals(NODE, captor.getValue().getDeviceCode());
        verify(vehiclePartMapper, never()).updatePo(any());
        verify(vehiclePartMapper, never()).selectPoByMap(any());
    }

    @Test
    @DisplayName("BIND 已有 vin+pn：仅更新命中行，不插入")
    void bind_existing_part_updates_only_matched_row() {
        VehiclePartPo existing = VehiclePartPo.builder()
                .id(7L).vin(VIN).pn(PN).deviceCode("ECU_OLD").build();
        when(vehiclePartMapper.selectPoListByVinAndPn(VIN, PN)).thenReturn(List.of(existing));

        consumer.onVehiclePartBindingChanged(record("BIND"));

        verify(vehiclePartMapper, never()).insertPo(any());
        verify(vehiclePartMapper).updatePo(existing);
        assertEquals(7L, existing.getId());
        assertEquals(NODE, existing.getDeviceCode());
    }

    @Test
    @DisplayName("并发插入命中唯一键：按 vin+pn 重查后转为更新，不丢事件")
    void duplicate_key_falls_back_to_update() {
        VehiclePartPo concurrent = VehiclePartPo.builder()
                .id(9L).vin(VIN).pn(PN).deviceCode(NODE).build();
        when(vehiclePartMapper.selectPoListByVinAndPn(VIN, PN)).thenReturn(List.of(), List.of(concurrent));
        doThrow(new DuplicateKeyException("uk_vehicle_part_vin_pn"))
                .when(vehiclePartMapper).insertPo(any());

        consumer.onVehiclePartBindingChanged(record("BIND"));

        verify(vehiclePartMapper, times(1)).insertPo(any());
        ArgumentCaptor<VehiclePartPo> captor = ArgumentCaptor.forClass(VehiclePartPo.class);
        verify(vehiclePartMapper).updatePo(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals(NODE, captor.getValue().getDeviceCode());
    }

    @Test
    @DisplayName("REPLACE 已有零件：更新投影，不重新插入")
    void replace_updates_existing_part() {
        VehiclePartPo existing = VehiclePartPo.builder()
                .id(7L).vin(VIN).pn(PN).deviceCode("ECU_OLD").build();
        when(vehiclePartMapper.selectPoListByVinAndPn(VIN, PN)).thenReturn(List.of(existing));

        consumer.onVehiclePartBindingChanged(record("REPLACE"));

        verify(vehiclePartMapper, never()).insertPo(any());
        verify(vehiclePartMapper).updatePo(existing);
        assertEquals(NODE, existing.getDeviceCode());
    }

    @Test
    @DisplayName("UNBIND：只删除该 vin+pn 命中的行，不波及全表")
    void unbind_deletes_only_matched_projection() {
        when(vehiclePartMapper.selectPoListByVinAndPn(VIN, PN)).thenReturn(List.of(
                VehiclePartPo.builder().id(1L).build(),
                VehiclePartPo.builder().id(2L).build()));

        consumer.onVehiclePartBindingChanged(record("UNBIND"));

        verify(vehiclePartMapper).selectPoListByVinAndPn(eq(VIN), eq(PN));
        verify(vehiclePartMapper, times(2)).physicalDeletePo(anyLong());
        verify(vehiclePartMapper, never()).insertPo(any());
        verify(vehiclePartMapper, never()).selectPoByMap(any());
    }

    @Test
    @DisplayName("查询异常：不得误删/误插，异常被记录后不影响后续消费")
    void query_failure_does_not_write() {
        when(vehiclePartMapper.selectPoListByVinAndPn(anyString(), anyString()))
                .thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(() -> consumer.onVehiclePartBindingChanged(record("BIND")));

        verify(vehiclePartMapper, never()).insertPo(any());
        verify(vehiclePartMapper, never()).updatePo(any());
        verify(vehiclePartMapper, never()).physicalDeletePo(anyLong());
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
