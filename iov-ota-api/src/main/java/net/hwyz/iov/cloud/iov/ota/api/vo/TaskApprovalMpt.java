package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

import java.time.Instant;

/**
 * 管理后台任务多级审批
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskApprovalMpt {

    private Long id;
    private Long taskId;
    private String level;
    private String approver;
    private String result;
    private String comment;
    private Instant decidedAt;
    private String approvalRef;

}