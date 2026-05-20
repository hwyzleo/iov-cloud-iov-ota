package net.hwyz.iov.cloud.iov.ota.service.domain.task.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.contract.enums.TaskRestrictionType;

import java.util.Date;

/**
 * 升级任务限制条件值对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRestrictionVo {

    /**
     * 主键
     */
    private Long id;

    /**
     * 升级任务ID
     */
    private Long taskId;

    /**
     * 限制条件类型
     */
    private TaskRestrictionType restrictionType;

    /**
     * 限制条件表达式
     */
    private String restrictionExpression;

    /**
     * 创建时间
     */
    private Date createTime;

}
