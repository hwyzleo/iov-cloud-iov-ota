package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * OTA Kafka Topic 统一配置绑定测试（CR-020 §7 配置绑定）
 *
 * <p>五个逻辑键只解析到目录精确名称，且可通过配置覆盖。
 *
 * @author hwyz_leo
 */
@DisplayName("OtaKafkaTopicsProperties 统一 Topic 映射（CR-020）")
class OtaKafkaTopicsPropertiesTest {

    @Test
    @DisplayName("默认值解析到 Kafka Topic 目录精确名称")
    void defaults_resolve_to_directory_names() {
        OtaKafkaTopicsProperties props = new OtaKafkaTopicsProperties();

        assertEquals("vagw.fota", props.getFotaUp());
        assertEquals("vagw.fota.dlq.up", props.getVagwUpDlq());
        assertEquals("ota.fota", props.getFotaDown());
        assertEquals("ota.fota.dlq.up", props.getFotaUpDlq());
        assertEquals("ota.vehicle-software-inventory.observed", props.getInventoryObserved());
        assertEquals("vmd.vehicle-produce", props.getVmdVehicleProduce());
        assertEquals("vmd.vehcile-part-binding.changed", props.getVmdPartBindingChanged());
    }

    @Test
    @DisplayName("配置覆盖后映射生效")
    void overridden_values_are_resolved() {
        OtaKafkaTopicsProperties props = new OtaKafkaTopicsProperties();
        props.setFotaUp("env.vagw.fota");
        props.setFotaDown("env.ota.fota");
        props.setVmdVehicleProduce("vmd.vehicle-produce.prod");

        assertEquals("env.vagw.fota", props.getFotaUp());
        assertEquals("env.ota.fota", props.getFotaDown());
        // 未覆盖项保持目录基线
        assertEquals("ota.vehicle-software-inventory.observed", props.getInventoryObserved());
        assertEquals("vmd.vehicle-produce.prod", props.getVmdVehicleProduce());
        assertEquals("vmd.vehcile-part-binding.changed", props.getVmdPartBindingChanged());
    }
}
