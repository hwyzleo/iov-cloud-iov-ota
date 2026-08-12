package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * 车辆 ECU 清单明细 PO（CR-012 §3）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_inventory_item")
public class VehicleInventoryItemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("inventory_id")
    private Long inventoryId;

    @TableField("ecu_id")
    private String ecuId;

    @TableField("ecu_name")
    private String ecuName;

    @TableField("software_pn")
    private String softwarePn;

    @TableField("software_version")
    private String softwareVersion;

    @TableField("hardware_pn")
    private String hardwarePn;

    @TableField("hardware_version")
    private String hardwareVersion;
}
