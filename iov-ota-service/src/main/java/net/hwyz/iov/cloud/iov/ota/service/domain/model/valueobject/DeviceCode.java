package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备代码值对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceCode implements Serializable {
    private String value;
}
