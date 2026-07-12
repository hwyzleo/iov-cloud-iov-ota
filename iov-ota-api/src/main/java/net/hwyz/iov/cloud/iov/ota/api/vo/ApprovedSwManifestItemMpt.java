package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台型式批准版本组合快照明细
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedSwManifestItemMpt {

    private Long id;
    private Long manifestId;
    private String vehicleNodeCode;
    private String partCode;
    private String approvedVersion;

}
