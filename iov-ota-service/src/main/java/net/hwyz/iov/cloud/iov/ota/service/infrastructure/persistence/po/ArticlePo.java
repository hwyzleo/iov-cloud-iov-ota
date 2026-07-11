package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

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
 * 文章表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-09-15
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_article")
public class ArticlePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章标题
     */
    @TableField("title")
    private String title;

    /**
     * 文章内容
     */
    @TableField("content")
    private String content;

    /**
     * 文章类型：1-活动条款，2-升级须知，3-隐私协议
     */
    @TableField("type")
    private Integer type;

    /**
     * 语言
     */
    @TableField("lang")
    private String lang;

    /**
     * 版本
     */
    @TableField("version")
    private String version;

    /**
     * 文章类型：RELEASE_NOTE / NOTICE / TERMS / PRIVACY
     */
    @TableField("article_type")
    private String articleType;

    /**
     * 状态：DRAFT / PUBLISHED
     */
    @TableField("status")
    private String status;
}
