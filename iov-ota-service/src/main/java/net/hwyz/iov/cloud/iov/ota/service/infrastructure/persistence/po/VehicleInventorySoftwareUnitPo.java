package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * 车辆 ECU 清单软件单元子表 PO（CR-019 §5.3）
 *
 * <p>UK(inventory_item_id, software_target_code, slot_key)。
 * SINGLE_IMAGE 在受理时也写一条 Target=ECU_IMAGE 的规范化 software unit；
 * 从此任务匹配、Execution 和事件发布对两种模式使用同一张子表。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_inventory_software_unit")
public class VehicleInventorySoftwareUnitPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("inventory_id")
    private Long inventoryId;

    @TableField("inventory_item_id")
    private Long inventoryItemId;

    @TableField("vin")
    private String vin;

    @TableField("ecu_id")
    private String ecuId;

    /** 软件目标编码（存量规范化为 ECU_IMAGE） */
    @TableField("software_target_code")
    private String softwareTargetCode;

    @TableField("software_part_number")
    private String softwarePartNumber;

    @TableField("sw_version")
    private String swVersion;

    /** 运行槽位（A/B；空=非 A/B 或缺省当前镜像） */
    @TableField("slot")
    private String slot;

    /** 槽位排序/唯一键（MySQL 生成列，不参与 Java 写入） */
    @TableField(value = "slot_key", exist = false)
    private String slotKey;

    @TableField("active")
    private Boolean active;

    /** 软件单元内容摘要（可空） */
    @TableField("digest")
    private String digest;
}
