package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 配置字值对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigWordVo {

    /**
     * 主键
     */
    private Long id;

    /**
     * 类型：1-固定配置字，2-软件零件版本
     */
    private Integer type;

    /**
     * 关联ID
     */
    private Long referenceId;

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 软件零件号
     */
    private String softwarePn;

    /**
     * 软件零件版本
     */
    private String softwarePartVer;

    /**
     * 配置字版本
     */
    private String configWordVersion;

    /**
     * 起始byte
     */
    private Integer startByte;

    /**
     * 起始bit
     */
    private Integer startBit;

    /**
     * 配置字值
     */
    private String configWordValue;

    /**
     * 依赖设备
     */
    private String dependDevice;

    /**
     * 依赖设备软件零件号
     */
    private String dependDeviceSoftwarePn;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
