package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.util.Date;

/**
 * OTA 车辆软件清单上报运行态 数据对象
 * <p>
 * CR-022：每 VIN 一行，保存最后一次由 OTA 成功受理的清单上报服务端时间；
 * 该值不参与 DIGEST 命中、任务匹配、安装许可与车辆主档投影（RD-022-1/2）。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_inventory_report_state")
public class VehicleInventoryReportStatePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车架号，运行态业务唯一键
     */
    @TableField("vin")
    private String vin;

    /**
     * 最后一次成功受理车辆软件清单上报的服务端时间
     */
    @TableField("last_reported_at")
    private Date lastReportedAt;
}
