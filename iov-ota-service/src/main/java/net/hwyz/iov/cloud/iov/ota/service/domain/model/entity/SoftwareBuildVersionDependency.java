package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;

import java.io.Serializable;

/**
 * 软件内部版本依赖实体
 */
@Data
@Builder
public class SoftwareBuildVersionDependency implements Serializable {
    private Long id;
    private SoftwareBuildVersionId softwareBuildVersionId;
    private SoftwareBuildVersionId dependencySoftwareBuildVersionId;
    private Integer adaptiveLevel;
    private Integer sort;
}
