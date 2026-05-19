package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务零部件信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePartExService {

    /**
     * 主键
     */
    private Long id;

    /**
     * 零件编号
     */
    private String pn;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 设备项
     */
    private String deviceItem;

    /**
     * 零件序列号
     */
    private String sn;

    /**
     * 配置字
     */
    private String configWord;

    /**
     * 供应商编码
     */
    private String supplierCode;

    /**
     * 批次号
     */
    private String batchNum;

    /**
     * 硬件版本号
     */
    private String hardwareVer;

    /**
     * 软件版本号
     */
    private String softwareVer;

    /**
     * 硬件零件号
     */
    private String hardwarePn;

    /**
     * 软件零件号
     */
    private String softwarePn;

    /**
     * 附加信息
     */
    private String extra;

}
