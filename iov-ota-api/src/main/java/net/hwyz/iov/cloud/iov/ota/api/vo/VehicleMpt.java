package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车辆信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 最后上报时间
     */
    private Date reportTime;

    /**
     * 最后基线代码
     */
    private String baselineCode;

    /**
     * 最后基线是否对齐
     */
    private Boolean baselineAlignment;

    /**
     * 最后设备信息
     */
    private String deviceInfo;

    /**
     * 最后升级活动ID
     */
    private Long activityId;

    /**
     * 最后升级任务ID
     */
    private Long taskId;

    /**
     * 最后升级配置字
     */
    private String configWord;

    /**
     * 最后OTA Master版本
     */
    private String masterVersion;

    /**
     * 创建时间
     */
    private Date createTime;

}
