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
import static org.mockito.Mockito.*;

/**
 * VMD 零件绑定事件消费者测试（CR-023 §6 / C1 并发唯一键）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VMD 零件绑定消费者（CR-023）")
class VmdVehiclePartBindingKafkaConsumerTest {

    @Mock private VehiclePartMapper vehiclePartMapper;

    private VmdVehiclePartBindingKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new VmdVehiclePartBindingKafkaConsumer(vehiclePartMapper, new ObjectMapper());
    }

    private ConsumerRecord<String, String> record(String changeType) {
        String json = "{\"vin\":\"LSVAU2188N2ZG4G\",\"partCode\":\"PN-1\","
                + "\"vehicleNodeCode\":\"ECU_A\",\"changeType\":\"" + changeType + "\"}";
        return new ConsumerRecord<>("vmd.vehicle-part-binding.changed", 0, 0L, "key", json);
    }

    @Test
    @DisplayName("BIND 新零件：插入投影")
    void bind_inserts_new_part() {
        when(vehiclePartMapper.selectPoByMap(any())).thenReturn(List.of());

        consumer.onVehiclePartBindingChanged(record("BIND"));

        ArgumentCaptor<VehiclePartPo> captor = ArgumentCaptor.forClass(VehiclePartPo.class);
        verify(vehiclePartMapper).insertPo(captor.capture());
        assertEquals("PN-1", captor.getValue().getPn());
        assertEquals("ECU_A", captor.getValue().getDeviceCode());
        verify(vehiclePartMapper, never()).updatePo(any());
    }

    @Test
    @DisplayName("并发插入命中唯一键：转为更新，不丢事件")
    void duplicate_key_falls_back_to_update() {
        VehiclePartPo concurrent = VehiclePartPo.builder()
                .id(9L).vin("LSVAU2188N2ZG4G").pn("PN-1").deviceCode("ECU_A").build();
        when(vehiclePartMapper.selectPoByMap(any())).thenReturn(List.of(), List.of(concurrent));
        doThrow(new DuplicateKeyException("uk_vehicle_part_vin_pn"))
                .when(vehiclePartMapper).insertPo(any());

        consumer.onVehiclePartBindingChanged(record("BIND"));

        verify(vehiclePartMapper, times(1)).insertPo(any());
        ArgumentCaptor<VehiclePartPo> captor = ArgumentCaptor.forClass(VehiclePartPo.class);
        verify(vehiclePartMapper).updatePo(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals("ECU_A", captor.getValue().getDeviceCode());
    }

    @Test
    @DisplayName("REPLACE 已有零件：更新投影，不重新插入")
    void replace_updates_existing_part() {
        VehiclePartPo existing = VehiclePartPo.builder()
                .id(7L).vin("LSVAU2188N2ZG4G").pn("PN-1").deviceCode("ECU_OLD").build();
        when(vehiclePartMapper.selectPoByMap(any())).thenReturn(List.of(existing));

        consumer.onVehiclePartBindingChanged(record("REPLACE"));

        verify(vehiclePartMapper, never()).insertPo(any());
        verify(vehiclePartMapper).updatePo(existing);
        assertEquals("ECU_A", existing.getDeviceCode());
    }

    @Test
    @DisplayName("UNBIND：物理删除该 vin+pn 投影，不残留重复键")
    void unbind_deletes_projection() {
        when(vehiclePartMapper.selectPoByMap(any())).thenReturn(List.of(
                VehiclePartPo.builder().id(1L).build(),
                VehiclePartPo.builder().id(2L).build()));

        consumer.onVehiclePartBindingChanged(record("UNBIND"));

        verify(vehiclePartMapper, times(2)).physicalDeletePo(anyLong());
        verify(vehiclePartMapper, never()).insertPo(any());
    }
}
