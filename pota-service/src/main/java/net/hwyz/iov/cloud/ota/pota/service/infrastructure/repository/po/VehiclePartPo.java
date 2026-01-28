package net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 车辆零件表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-28
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_part")
public class VehiclePartPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 零件编号
     */
    @TableField("pn")
    private String pn;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 设备代码
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 设备项
     */
    @TableField("device_item")
    private String deviceItem;

    /**
     * 零件序列号
     */
    @TableField("sn")
    private String sn;

    /**
     * 配置字
     */
    @TableField("config_word")
    private String configWord;

    /**
     * 供应商编码
     */
    @TableField("supplier_code")
    private String supplierCode;

    /**
     * 批次号
     */
    @TableField("batch_num")
    private String batchNum;

    /**
     * 硬件版本号
     */
    @TableField("hardware_ver")
    private String hardwareVer;

    /**
     * 软件版本号
     */
    @TableField("software_ver")
    private String softwareVer;

    /**
     * 硬件零件号
     */
    @TableField("hardware_pn")
    private String hardwarePn;

    /**
     * 软件零件号
     */
    @TableField("software_pn")
    private String softwarePn;

    /**
     * 附加信息
     */
    @TableField("extra")
    private String extra;
}
