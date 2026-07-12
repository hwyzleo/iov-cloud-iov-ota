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
 * 升级活动多级审批表 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_activity_approval")
public class ActivityApprovalPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 升级活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 审批阶段：QUALITY / PRODUCT / SECURITY
     */
    @TableField("approval_stage")
    private String approvalStage;

    /**
     * 审批人ID
     */
    @TableField("approver_id")
    private String approverId;

    /**
     * 审批结果：PASS / REJECT
     */
    @TableField("result")
    private String result;

    /**
     * 审批意见
     */
    @TableField("comment")
    private String comment;

    /**
     * 审批时间
     */
    @TableField("approve_time")
    private Date approveTime;

}
