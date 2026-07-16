package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskStrategyType;

import java.util.Date;

/**
 * 升级任务策略值对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStrategyVo {

    /**
     * 主键
     */
    private Long id;

    /**
     * 升级任务ID
     */
    private Long taskId;

    /**
     * 策略类型
     */
    private TaskStrategyType strategyType;

    /**
     * 策略表达式
     */
    private String strategyExpression;

    /**
     * 下载重试最大次数
     */
    private Integer downloadRetryMax;

    /**
     * 重试退避策略：FIXED/EXP
     */
    private String retryBackoff;

    /**
     * 断电后是否续传
     */
    private Boolean resumeOnPoweroff;

    /**
     * 创建时间
     */
    private Date createTime;

}
