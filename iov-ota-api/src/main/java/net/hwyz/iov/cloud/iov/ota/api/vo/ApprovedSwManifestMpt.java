package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

import java.util.Date;
import java.util.List;

/**
 * 管理后台型式批准版本组合快照
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedSwManifestMpt {

    private Long id;
    private String manifestCode;
    private Long activityId;
    private String swinCode;
    private String rxswinValue;
    private String manifestStatus;
    private Date approveTime;
    private List<ApprovedSwManifestItemMpt> items;

}
