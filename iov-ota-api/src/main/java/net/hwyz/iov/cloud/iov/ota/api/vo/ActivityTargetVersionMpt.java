package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台活动目标版本组合
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTargetVersionMpt {

    private Long id;
    private Long activityId;
    private String baselineCode;
    private String vehicleNodeCode;
    private String partCode;
    private String targetSoftwareBuildVer;

}
