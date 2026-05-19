package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 软件零件号值对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoftwarePn implements Serializable {
    private String value;
}
