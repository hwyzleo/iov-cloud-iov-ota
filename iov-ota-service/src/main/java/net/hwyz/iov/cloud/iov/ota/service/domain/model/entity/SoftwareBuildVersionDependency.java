package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.PotaBaseException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;

import java.io.Serializable;

/**
 * 软件内部版本依赖实体（充血模型）
 */
@Data
@Builder
public class SoftwareBuildVersionDependency implements Serializable {
    private Long id;
    private SoftwareBuildVersionId softwareBuildVersionId;
    private SoftwareBuildVersionId dependencySoftwareBuildVersionId;
    private Integer adaptiveLevel;
    private Integer sort;

    /**
     * 设置适配级别
     * 业务规则：适配级别必须在0-100之间
     */
    public void setAdaptiveLevel(Integer level) {
        if (level == null || level < 0 || level > 100) {
            throw new PotaBaseException("适配级别必须在0-100之间");
        }
        this.adaptiveLevel = level;
    }
}
