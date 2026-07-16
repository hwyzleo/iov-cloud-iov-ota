package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台升级任务匹配条件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRestrictionMpt {

    /**
     * 主键
     */
    private Long id;

    /**
     * 限制类型（TaskRestrictionType 枚举名）
     */
    private String type;

    /**
     * 限制表达式
     */
    private String expression;
}
