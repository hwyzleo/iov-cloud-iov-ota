package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 车辆主档本地只读投影 数据对象
 * <p>
 * CR-011: 消费 MDM/VMD VehicleProduceEvent，以 VIN + 上游版本幂等 upsert
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_projection")
public class VehicleProjectionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车架号，车辆投影业务主键
     */
    @TableField("vin")
    private String vin;

    /**
     * 车辆生产业务事实时间
     */
    @TableField("production_time")
    private Date productionTime;

    /**
     * 生产工厂编码
     */
    @TableField("plant_code")
    private String plantCode;

    /**
     * 品牌编码
     */
    @TableField("brand_code")
    private String brandCode;

    /**
     * 平台编码
     */
    @TableField("platform_code")
    private String platformCode;

    /**
     * 车系编码
     */
    @TableField("car_line_code")
    private String carLineCode;

    /**
     * 车型编码
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 版本编码
     */
    @TableField("variant_code")
    private String variantCode;

    /**
     * 配置编码，OTA基线和圈车核心锚点
     */
    @TableField("configuration_code")
    private String configurationCode;

    /**
     * 最近一次生效的上游事件ID
     */
    @TableField("source_event_id")
    private String sourceEventId;

    /**
     * MDM/VMD车辆主档版本
     */
    @TableField("source_version")
    private Long sourceVersion;

    /**
     * 上游事件发生时间
     */
    @TableField("source_event_time")
    private Date sourceEventTime;

    /**
     * 投影最后同步时间
     */
    @TableField("last_sync_time")
    private Date lastSyncTime;
}
