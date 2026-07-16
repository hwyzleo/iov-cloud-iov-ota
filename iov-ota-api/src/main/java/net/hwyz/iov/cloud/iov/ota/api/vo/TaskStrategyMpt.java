package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台升级任务策略
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStrategyMpt {

    /**
     * 主键
     */
    private Long id;

    /**
     * 策略类型（TaskStrategyType 枚举名）
     */
    private String type;

    /**
     * 策略内容
     */
    private String strategy;
}
