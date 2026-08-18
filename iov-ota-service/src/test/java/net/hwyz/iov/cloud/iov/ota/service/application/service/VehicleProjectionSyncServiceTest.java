package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.RetryableProjectionException;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleProjectionNotFoundException;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleProjectionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.VmdVehicleProjectionGateway;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto.VmdVehicleProjectionDto;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto.VmdVehicleProjectionSnapshotDto;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.DataSyncRecordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.DataSyncRecordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CR-015 P1-B 车辆投影回源/Bootstrap/对账测试（§8 投影验收）
 * <p>覆盖：本地缺失回源、VMD 不存在/不可用区分、旧版本不覆盖新版本、Bootstrap 断点恢复、事件并发（并发插入回退）。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleProjectionSyncService 回源/Bootstrap/对账")
class VehicleProjectionSyncServiceTest {

    @Mock private VehicleProjectionRepository vehicleProjectionRepository;
    @Mock private VmdVehicleProjectionGateway vmdVehicleProjectionGateway;
    @Mock private DataSyncRecordMapper dataSyncRecordMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private VehicleProjectionSyncService service;

    @BeforeEach
    void setUp() {
        // 无 checkpoint
        when(dataSyncRecordMapper.selectPoByMap(any())).thenReturn(List.of());
        // @Value 字段在单测中手动注入
        ReflectionTestUtils.setField(service, "bootstrapPageSize", 500);
        ReflectionTestUtils.setField(service, "bootstrapMaxPages", 1000);
    }

    private VmdVehicleProjectionDto dto(String vin, long version) {
        return VmdVehicleProjectionDto.builder()
                .vin(vin)
                .productionTime("2026-08-01T00:00:00Z")
                .configurationCode("CFG-1")
                .variantCode("VAR-1")
                .sourceVersion(version)
                .build();
    }

    @Test
    @DisplayName("本地缺失 -> VMD 回源插入投影，返回 FOUND")
    void syncByVin_localMissing_insert() {
        String vin = "LSV0000000000000001";
        when(vmdVehicleProjectionGateway.getByVin(vin)).thenReturn(dto(vin, 3L));
        when(vehicleProjectionRepository.findByVin(vin)).thenReturn(Optional.empty());

        VehicleProjectionPo result = service.syncByVin(vin);

        assertNotNull(result);
        assertEquals(vin, result.getVin());
        verify(vehicleProjectionRepository).insert(any(VehicleProjectionPo.class));
    }

    @Test
    @DisplayName("VMD 明确不存在（404）-> 返回 null，不落库，不视为可重试")
    void syncByVin_notFound_returnsNull() {
        String vin = "LSV0000000000000001";
        when(vmdVehicleProjectionGateway.getByVin(vin))
                .thenThrow(new VehicleProjectionNotFoundException(vin));

        assertNull(service.syncByVin(vin));
        verify(vehicleProjectionRepository, never()).insert(any());
    }

    @Test
    @DisplayName("VMD 服务不可用 -> 抛 RetryableProjectionException，不得缓存为 NOT_FOUND")
    void syncByVin_unavailable_throwsRetryable() {
        String vin = "LSV0000000000000001";
        when(vmdVehicleProjectionGateway.getByVin(vin))
                .thenThrow(new RetryableProjectionException("VMD 不可用"));

        assertThrows(RetryableProjectionException.class, () -> service.syncByVin(vin));
        verify(vehicleProjectionRepository, never()).insert(any());
    }

    @Test
    @DisplayName("旧版本不覆盖新版本：本地 sourceVersion 更大时 updateIfNewerVersion=false")
    void syncByVin_olderRemote_notOverwrite() {
        String vin = "LSV0000000000000001";
        when(vmdVehicleProjectionGateway.getByVin(vin)).thenReturn(dto(vin, 3L)); // 远端较旧

        VehicleProjectionPo local = new VehicleProjectionPo();
        local.setVin(vin);
        local.setSourceVersion(10L);
        when(vehicleProjectionRepository.findByVin(vin)).thenReturn(Optional.of(local));
        when(vehicleProjectionRepository.updateIfNewerVersion(any())).thenReturn(false);

        VehicleProjectionPo result = service.syncByVin(vin);

        assertNotNull(result);
        assertEquals(10L, result.getSourceVersion()); // 保留本地新版本
        verify(vehicleProjectionRepository).updateIfNewerVersion(any(VehicleProjectionPo.class));
    }

    @Test
    @DisplayName("回源 VIN 与请求不一致 -> 拒绝")
    void syncByVin_vinMismatch_rejected() {
        String vin = "LSV0000000000000001";
        when(vmdVehicleProjectionGateway.getByVin(vin)).thenReturn(dto("OTHER-VIN", 1L));

        assertThrows(IllegalStateException.class, () -> service.syncByVin(vin));
        verify(vehicleProjectionRepository, never()).insert(any());
    }

    @Test
    @DisplayName("Bootstrap：游标分页拉取、幂等 upsert、保存 checkpoint、返回统计")
    void bootstrap_paginatesAndUpserts() {
        String vin1 = "LSV0000000000000001";
        String vin2 = "LSV0000000000000002";
        when(vmdVehicleProjectionGateway.getSnapshot(null, 500, 0L))
                .thenReturn(VmdVehicleProjectionSnapshotDto.builder()
                        .items(List.of(dto(vin1, 1L)))
                        .nextCursor("c2")
                        .hasMore(true)
                        .build());
        when(vmdVehicleProjectionGateway.getSnapshot("c2", 500, 0L))
                .thenReturn(VmdVehicleProjectionSnapshotDto.builder()
                        .items(List.of(dto(vin2, 1L)))
                        .nextCursor(null)
                        .hasMore(false)
                        .build());
        when(vehicleProjectionRepository.findByVin(anyString())).thenReturn(Optional.empty());

        VehicleProjectionSyncService.ProjectionSyncStats stats = service.bootstrapFromSnapshot();

        assertEquals(2, stats.getScan());
        assertEquals(2, stats.getAdded());
        verify(vehicleProjectionRepository, times(2)).insert(any());
        verify(dataSyncRecordMapper).insertPo(any()); // checkpoint 落库
    }

    @Test
    @DisplayName("Bootstrap：服务不可用中断，保留 checkpoint，不丢已处理页")
    void bootstrap_unavailable_keepsCheckpoint() {
        String vin1 = "LSV0000000000000001";
        when(vmdVehicleProjectionGateway.getSnapshot(null, 500, 0L))
                .thenReturn(VmdVehicleProjectionSnapshotDto.builder()
                        .items(List.of(dto(vin1, 1L)))
                        .nextCursor("c2")
                        .hasMore(true)
                        .build());
        when(vmdVehicleProjectionGateway.getSnapshot("c2", 500, 0L))
                .thenThrow(new RetryableProjectionException("VMD 不可用"));
        when(vehicleProjectionRepository.findByVin(anyString())).thenReturn(Optional.empty());

        VehicleProjectionSyncService.ProjectionSyncStats stats = service.bootstrapFromSnapshot();

        assertEquals(1, stats.getAdded()); // 第一页已处理
        verify(dataSyncRecordMapper).insertPo(any()); // 断点仍保存
    }

    @Test
    @DisplayName("对账：更新/忽略统计与最大版本差")
    void reconcile_countsUpdateIgnoreAndVersionDiff() {
        String vin = "LSV0000000000000001";
        // 已有 checkpoint -> 增量 updatedAfter=1000，且保存时走 updatePo
        DataSyncRecordPo existingCheckpoint = new DataSyncRecordPo();
        existingCheckpoint.setId(1L);
        existingCheckpoint.setSource(9);
        existingCheckpoint.setType(9);
        existingCheckpoint.setCode("vehicle-projection-sync");
        existingCheckpoint.setData("{\"cursor\":\"c5\",\"updatedAfter\":1000}");
        when(dataSyncRecordMapper.selectPoByMap(any())).thenReturn(List.of(existingCheckpoint));

        when(vmdVehicleProjectionGateway.getSnapshot(null, 500, 1000L))
                .thenReturn(VmdVehicleProjectionSnapshotDto.builder()
                        .items(List.of(dto(vin, 5L)))
                        .nextCursor(null)
                        .hasMore(false)
                        .build());

        VehicleProjectionPo local = new VehicleProjectionPo();
        local.setVin(vin);
        local.setSourceVersion(2L); // 本地旧
        when(vehicleProjectionRepository.findByVin(vin)).thenReturn(Optional.of(local));
        when(vehicleProjectionRepository.updateIfNewerVersion(any())).thenReturn(true);

        VehicleProjectionSyncService.ProjectionSyncStats stats = service.reconcile();

        assertEquals(1, stats.getScan());
        assertEquals(1, stats.getUpdated());
        verify(dataSyncRecordMapper).updatePo(any()); // checkpoint 更新（已存在记录）
    }

    @Test
    @DisplayName("处理车辆生产事件：旧事件不覆盖新事件")
    void handleVehicleProduceEvent_oldEvent_skip() {
        String vin = "LSV0000000000000001";
        VehicleProjectionPo local = new VehicleProjectionPo();
        local.setVin(vin);
        local.setSourceVersion(10L);
        when(vehicleProjectionRepository.findByVinForUpdate(vin)).thenReturn(Optional.of(local));

        String oldEvent = "{\"eventId\":\"e1\",\"eventType\":\"VehicleProduceEvent\",\"aggregateId\":\"" + vin + "\","
                + "\"version\":3,\"occurredAt\":\"2026-08-01T00:00:00\","
                + "\"payload\":{\"vin\":\"" + vin + "\",\"produceTime\":\"2026-08-01T00:00:00\",\"configurationCode\":\"CFG-1\"}}";
        service.handleVehicleProduceEvent(oldEvent);

        verify(vehicleProjectionRepository, never()).update(any());
    }
}
