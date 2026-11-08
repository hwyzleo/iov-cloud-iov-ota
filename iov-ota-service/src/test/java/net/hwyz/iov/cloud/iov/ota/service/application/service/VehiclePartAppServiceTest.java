package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartHistoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartHistoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 车辆零件保存修复测试（CR-023 §6）
 *
 * <p>覆盖：新增、11 个受管字段逐项变更、无变化幂等、NULL/空串等价、
 * 大小写敏感、增量不删除、并发唯一键转更新、事务与历史内容。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehiclePartAppService 零件全字段更新与历史（CR-023）")
class VehiclePartAppServiceTest {

    private static final String VIN = "LSVAU2188N2ZG4G";
    private static final String PN = "PN-001";

    @Mock private VehiclePartMapper vehiclePartMapper;
    @Mock private VehiclePartHistoryMapper vehiclePartHistoryMapper;

    @InjectMocks
    private VehiclePartAppService vehiclePartAppService;

    @Captor private ArgumentCaptor<VehiclePartHistoryPo> historyCaptor;

    private VehiclePartPo basePart() {
        return VehiclePartPo.builder()
                .id(1L).vin(VIN).pn(PN)
                .deviceCode("ECU_A").deviceItem("ITEM-1").sn("SN-1")
                .configWord("CFG-1").supplierCode("SUP-1").batchNum("BATCH-1")
                .hardwareVer("HW-V1").softwareVer("SW-V1")
                .hardwarePn("HPN-1").softwarePn("SPN-1").extra("EXTRA-1")
                .build();
    }

    static Stream<Arguments> managedFieldMutators() {
        return Stream.of(
                Arguments.of("deviceCode", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setDeviceCode),
                Arguments.of("deviceItem", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setDeviceItem),
                Arguments.of("sn", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setSn),
                Arguments.of("configWord", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setConfigWord),
                Arguments.of("supplierCode", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setSupplierCode),
                Arguments.of("batchNum", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setBatchNum),
                Arguments.of("hardwareVer", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setHardwareVer),
                Arguments.of("softwareVer", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setSoftwareVer),
                Arguments.of("hardwarePn", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setHardwarePn),
                Arguments.of("softwarePn", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setSoftwarePn),
                Arguments.of("extra", (BiConsumer<VehiclePartPo, String>) VehiclePartPo::setExtra));
    }

    @ParameterizedTest(name = "受管字段变化即更新并留痕：{0}")
    @MethodSource("managedFieldMutators")
    @DisplayName("每个受管字段变化均更新投影并写 CHANGE 历史（不仅 SN）")
    void any_managed_field_change_updates_and_records_history(
            String field, BiConsumer<VehiclePartPo, String> mutator) {
        VehiclePartPo current = basePart();
        VehiclePartPo incoming = basePart();
        mutator.accept(incoming, "CHANGED");
        when(vehiclePartMapper.selectPoByVinAndPn(VIN, PN)).thenReturn(current);

        vehiclePartAppService.saveVehicleParts(VIN, "REPLAY", List.of(incoming));

        ArgumentCaptor<VehiclePartPo> updated = ArgumentCaptor.forClass(VehiclePartPo.class);
        verify(vehiclePartMapper, times(1)).updateManagedFields(updated.capture());
        assertEquals("CHANGED", valueOf(updated.getValue(), field), "更新后的投影必须包含新值");
        assertEquals(VIN, updated.getValue().getVin(), "vin 身份键不得改写");
        assertEquals(PN, updated.getValue().getPn(), "pn 身份键不得改写");

        verify(vehiclePartHistoryMapper, times(1)).insertPo(historyCaptor.capture());
        VehiclePartHistoryPo history = historyCaptor.getValue();
        assertTrue(history.getDescription().startsWith("REPLAY变更:"), "历史必须保留请求 remark");
        assertTrue(history.getDescription().contains(field), "历史必须记录变更字段名称");
        assertEquals("CHANGED", valueOf(history, field), "历史保存变更后完整快照");
    }

    @Test
    @DisplayName("无变化：幂等成功，不更新、不写历史")
    void unchanged_is_idempotent() {
        when(vehiclePartMapper.selectPoByVinAndPn(VIN, PN)).thenReturn(basePart());

        vehiclePartAppService.saveVehicleParts(VIN, "REPLAY", List.of(basePart()));

        verify(vehiclePartMapper, never()).updateManagedFields(any());
        verify(vehiclePartMapper, never()).insertPo(any());
        verify(vehiclePartHistoryMapper, never()).insertPo(any());
    }

    @Test
    @DisplayName("NULL 与空串等价：不因无意义格式波动反复写历史")
    void null_and_empty_are_treated_as_equal() {
        VehiclePartPo current = basePart();
        current.setDeviceItem(null);
        current.setExtra("");
        VehiclePartPo incoming = basePart();
        incoming.setDeviceItem("");
        incoming.setExtra(null);
        when(vehiclePartMapper.selectPoByVinAndPn(VIN, PN)).thenReturn(current);

        vehiclePartAppService.saveVehicleParts(VIN, null, List.of(incoming));

        verify(vehiclePartMapper, never()).updateManagedFields(any());
        verify(vehiclePartHistoryMapper, never()).insertPo(any());
    }

    @Test
    @DisplayName("大小写差异视为有业务意义：case-only 变化必须更新")
    void case_only_change_is_not_ignored() {
        VehiclePartPo current = basePart();
        VehiclePartPo incoming = basePart();
        incoming.setSn("sn-1");
        when(vehiclePartMapper.selectPoByVinAndPn(VIN, PN)).thenReturn(current);

        vehiclePartAppService.saveVehicleParts(VIN, null, List.of(incoming));

        verify(vehiclePartMapper, times(1)).updateManagedFields(any());
        verify(vehiclePartHistoryMapper, times(1)).insertPo(any());
    }

    @Test
    @DisplayName("新增：保存完整受管字段并写 ADD 历史")
    void insert_new_part_records_add_history() {
        when(vehiclePartMapper.selectPoByVinAndPn(VIN, PN)).thenReturn(null);

        vehiclePartAppService.saveVehicleParts(VIN, "IMPORT", List.of(basePart()));

        ArgumentCaptor<VehiclePartPo> inserted = ArgumentCaptor.forClass(VehiclePartPo.class);
        verify(vehiclePartMapper, times(1)).insertPo(inserted.capture());
        VehiclePartPo po = inserted.getValue();
        assertEquals(VIN, po.getVin());
        assertEquals(PN, po.getPn());
        assertEquals("ECU_A", po.getDeviceCode());
        assertEquals("CFG-1", po.getConfigWord());
        assertEquals("EXTRA-1", po.getExtra());
        verify(vehiclePartHistoryMapper, times(1)).insertPo(historyCaptor.capture());
        assertEquals("IMPORT新增", historyCaptor.getValue().getDescription());
    }

    @Test
    @DisplayName("并发插入命中唯一键：转为变更更新，不丢同步")
    void duplicate_key_on_insert_falls_back_to_update() {
        VehiclePartPo existing = basePart();
        existing.setConfigWord("CFG-OLD");
        // 第一次查询未命中 → 走插入；插入被唯一键拦截；二次查询命中并发写入的行
        when(vehiclePartMapper.selectPoByVinAndPn(VIN, PN)).thenReturn(null, existing);
        doThrow(new DuplicateKeyException("uk_vehicle_part_vin_pn"))
                .when(vehiclePartMapper).insertPo(any());

        VehiclePartPo incoming = basePart();
        incoming.setConfigWord("CFG-NEW");
        assertDoesNotThrow(() -> vehiclePartAppService.saveVehicleParts(VIN, "VMD", List.of(incoming)));

        verify(vehiclePartMapper, never()).updateManagedFields(argThat(po -> "CFG-OLD".equals(po.getConfigWord())));
        verify(vehiclePartMapper, times(1)).updateManagedFields(any());
        verify(vehiclePartHistoryMapper, times(1)).insertPo(historyCaptor.capture());
        assertTrue(historyCaptor.getValue().getDescription().contains("configWord"));
    }

    @Test
    @DisplayName("增量语义：请求中未出现的零件不删除，不新增整车快照写接口")
    void never_deletes_parts_not_in_request() {
        when(vehiclePartMapper.selectPoByVinAndPn(VIN, PN)).thenReturn(basePart());

        vehiclePartAppService.saveVehicleParts(VIN, null, List.of(basePart()));

        verify(vehiclePartMapper, never()).physicalDeletePo(any());
        verify(vehiclePartMapper, never()).logicalDeletePo(any());
        verify(vehiclePartMapper, never()).batchPhysicalDeletePo(any());
    }

    @Test
    @DisplayName("pn 缺失：身份键不完整时显式失败")
    void blank_pn_fails_fast() {
        VehiclePartPo incoming = basePart();
        incoming.setPn(" ");
        assertThrows(IllegalArgumentException.class,
                () -> vehiclePartAppService.saveVehicleParts(VIN, null, List.of(incoming)));
        verifyNoInteractions(vehiclePartMapper);
    }

    @Test
    @DisplayName("事务：saveVehicleParts 与历史写入同事务，异常回滚")
    void saveVehicleParts_is_transactional() throws NoSuchMethodException {
        Method method = VehiclePartAppService.class.getMethod(
                "saveVehicleParts", String.class, String.class, List.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, "saveVehicleParts 必须标注 @Transactional");
        assertTrue(Arrays.asList(transactional.rollbackFor()).contains(Exception.class),
                "必须回滚受检/运行时异常，避免部分提交");
    }

    @Test
    @DisplayName("脱敏与历史内容：描述不包含完整 VIN 或原始配置字，maskVin 不返回完整 VIN")
    void logs_and_history_do_not_leak_vin_or_config_word() throws Exception {
        VehiclePartPo current = basePart();
        VehiclePartPo incoming = basePart();
        incoming.setConfigWord("SECRET-CONFIG-WORD");
        when(vehiclePartMapper.selectPoByVinAndPn(VIN, PN)).thenReturn(current);

        vehiclePartAppService.saveVehicleParts(VIN, "VMD", List.of(incoming));

        verify(vehiclePartHistoryMapper).insertPo(historyCaptor.capture());
        String description = historyCaptor.getValue().getDescription();
        assertFalse(description.contains(VIN), "变更描述不得输出完整 VIN");
        assertFalse(description.contains("SECRET-CONFIG-WORD"), "变更描述不得输出原始配置字");

        Method maskVin = VehiclePartAppService.class.getDeclaredMethod("maskVin", String.class);
        maskVin.setAccessible(true);
        assertEquals("***", maskVin.invoke(null, "ABC"));
        String masked = (String) maskVin.invoke(null, VIN);
        assertFalse(masked.contains(VIN), "日志 VIN 必须脱敏：" + masked);
        assertTrue(masked.startsWith(VIN.substring(0, 3)));
    }

    private static Object valueOf(Object po, String field) {
        try {
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            return po.getClass().getMethod(getter).invoke(po);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法读取字段: " + field, e);
        }
    }
}
