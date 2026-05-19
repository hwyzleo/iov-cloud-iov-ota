package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 对外服务固定配置字
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatiblePnExService {

    /**
     * 主键
     */
    private Long id;

    /**
     * 类型：1-软件零件号，2-硬件零件号
     */
    private Integer type;

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 零件号
     */
    private String pn;

    /**
     * 兼容零件号
     */
    private String compatiblePn;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
