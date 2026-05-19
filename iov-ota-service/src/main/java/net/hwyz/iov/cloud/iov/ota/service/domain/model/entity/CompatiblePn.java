package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 兼件号实体
 */
@Data
@Builder
public class CompatiblePn implements Serializable {
    private Long id;
    private String partCode;
    private String compatiblePn;
    private String partName;
    private String description;
    private Integer status;
    private Instant createTime;
}
