package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车辆升级任务
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskVehicleMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 升级活动ID
     */
    private Long activityId;

    /**
     * 升级活动名称
     */
    private String activityName;

    /**
     * 升级任务ID
     */
    private Long taskId;

    /**
     * 升级任务名称
     */
    private String taskName;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 车辆任务状态
     */
    private Integer state;

    /**
     * 结果代码
     */
    private String resultCode;

    /**
     * 创建时间
     */
    private Date createTime;

}
