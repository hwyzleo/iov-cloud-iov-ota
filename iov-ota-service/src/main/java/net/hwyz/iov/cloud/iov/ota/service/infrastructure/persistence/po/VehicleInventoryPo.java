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
 * 车辆 ECU 清单头 PO（CR-012 §3）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_inventory")
public class VehicleInventoryPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("vin")
    private String vin;

    @TableField("inventory_revision")
    private Long inventoryRevision;

    @TableField("digest")
    private String digest;

    @TableField("algorithm")
    private String algorithm;

    /** canonicalization 版本（v1/v2） */
    @TableField("canonicalization_version")
    private Integer canonicalizationVersion;

    /** canonicalization-v2 摘要（SHA-256 bytes） */
    @TableField("canonical_digest")
    private byte[] canonicalDigest;

    /** 车端清单采集时间（source_collected_at，与服务端 accepted_time 分列） */
    @TableField("source_collected_at")
    private Date sourceCollectedAt;

    @TableField("accepted_time")
    private Date acceptedTime;
}
