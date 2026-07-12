package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台活动安装顺序
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityInstallOrderMpt {

    private Long id;
    private Long activityId;
    private String vehicleNodeCode;
    private Integer seqNo;
    private Integer parallelGroup;

}
