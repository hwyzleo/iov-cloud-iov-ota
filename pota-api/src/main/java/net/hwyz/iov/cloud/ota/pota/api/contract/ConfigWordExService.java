package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 对外服务配置字
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigWordExService {

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
     * 依赖零部件ECU
     */
    private String dependEcu;

    /**
     * 依赖ECU软件零件号
     */
    private String dependEcuSoftwarePn;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
