package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台配置字
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigWordMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 配置字代码
     */
    private String code;

    /**
     * 配置字名称
     */
    private String name;

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 数据格式
     */
    private String dataFormat;

    /**
     * 数据长度
     */
    private Integer byteLength;

    /**
     * 读写能力
     */
    private String rwCapability;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
