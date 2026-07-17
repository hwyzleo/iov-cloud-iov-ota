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
     * 车辆生产业务事实时间
     */
    private Date productionTime;

    /**
     * 生产工厂编码
     */
    private String plantCode;

    /**
     * 品牌编码
     */
    private String brandCode;

    /**
     * 平台编码
     */
    private String platformCode;

    /**
     * 车系编码
     */
    private String carLineCode;

    /**
     * 车型编码
     */
    private String modelCode;

    /**
     * 版本编码
     */
    private String variantCode;

    /**
     * 配置编码，OTA基线和圈车核心锚点
     */
    private String configurationCode;

    /**
     * 最近一次生效的上游事件ID
     */
    private String sourceEventId;

    /**
     * MDM/VMD车辆主档版本
     */
    private Long sourceVersion;

    /**
     * 上游事件发生时间
     */
    private Date sourceEventTime;

    /**
     * 投影最后同步时间
     */
    private Date lastSyncTime;

    /**
     * 创建时间
     */
    private Date createTime;

}
