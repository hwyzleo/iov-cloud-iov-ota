package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 兼件号结果DTO
 */
@Data
@Builder
public class CompatiblePnDto {
    private Long id;
    private String partCode;
    private String compatiblePn;
    private String partName;
    private String description;
    private Integer status;
    private Instant createTime;
}
