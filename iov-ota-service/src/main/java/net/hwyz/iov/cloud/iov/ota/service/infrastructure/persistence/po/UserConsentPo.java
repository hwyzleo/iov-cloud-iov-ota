package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.Date;

/**
 * tb_user_consent 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_user_consent")
public class UserConsentPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 升级任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 授权类型
     */
    @TableField("consent_type")
    private String consentType;

    /**
     * 文章ID
     */
    @TableField("article_id")
    private Long articleId;

    /**
     * 授权结果：0-拒绝，1-同意
     */
    @TableField("consent_result")
    private Integer consentResult;

    /**
     * 授权时间
     */
    @TableField("consent_time")
    private Date consentTime;

}
