package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 软件内部版本ID值对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoftwareBuildVersionId implements Serializable {
    private Long value;
}
