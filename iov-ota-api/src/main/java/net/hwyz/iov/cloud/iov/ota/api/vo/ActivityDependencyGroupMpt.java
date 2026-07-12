package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台活动同升同降依赖组
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDependencyGroupMpt {

    private Long id;
    private Long activityId;
    private String groupCode;
    private String memberNodeCode;
    private Boolean rollbackTogether;

}
