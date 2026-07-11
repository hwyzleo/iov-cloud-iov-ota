package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台基线
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaselineMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 基线代码
     */
    private String baselineCode;

    /**
     * 基线名称
     */
    private String name;

    /**
     * 锚定类型
     */
    private String anchorType;

    /**
     * 锚定代码
     */
    private String anchorCode;

    /**
     * 基线版本
     */
    private String baselineVersion;

    /**
     * 基线状态
     */
    private String baselineStatus;

    /**
     * 数据来源
     */
    private String source;

    /**
     * 最后同步时间
     */
    private Date syncTime;

    /**
     * 创建时间
     */
    private Date createTime;

}
