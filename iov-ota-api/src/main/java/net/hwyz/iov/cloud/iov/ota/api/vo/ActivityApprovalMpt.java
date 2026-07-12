package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

import java.util.Date;

/**
 * 管理后台活动多级审批
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityApprovalMpt {

    private Long id;
    private Long activityId;
    private String approvalStage;
    private String approverId;
    private String result;
    private String comment;
    private Date approveTime;
    private Date createTime;

}
