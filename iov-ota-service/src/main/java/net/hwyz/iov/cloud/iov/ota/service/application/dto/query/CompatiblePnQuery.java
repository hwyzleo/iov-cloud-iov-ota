package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;

/**
 * 兼件号查询条件
 */
@Data
public class CompatiblePnQuery {
    private String partCode;
    private String compatiblePn;
    private String partName;
    private Integer status;
}
