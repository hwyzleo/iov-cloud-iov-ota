package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台升级活动下固定配置字信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActivityFixedConfigWordMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 升级活动ID
     */
    private Long activityId;

    /**
     * 固定配置字ID
     */
    private Long fixedConfigWordId;

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 软件零件号
     */
    private String softwarePn;

    /**
     * 分类
     */
    private Integer type;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
