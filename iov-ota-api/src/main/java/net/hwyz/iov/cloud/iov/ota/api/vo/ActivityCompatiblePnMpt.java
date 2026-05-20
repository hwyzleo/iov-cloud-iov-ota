package net.hwyz.iov.cloud.iov.ota.api.contract;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.web.domain.BaseRequest;

import java.util.Date;

/**
 * 管理后台升级活动下软件内部版本信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActivityCompatiblePnMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 兼容零件号ID
     */
    private Long compatiblePnId;

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
