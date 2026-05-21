package net.hwyz.iov.cloud.iov.ota.api.contract;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台文章
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArticleMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章内容
     */
    private String content;

    /**
     * 文章类型：1-活动条款，2-升级须知，3-隐私协议
     */
    private Integer type;

    /**
     * 创建时间
     */
    private Date createTime;

}
