package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;
import java.util.List;

/**
 * 管理后台升级任务
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * @deprecated 废弃于 US-061，车辆录入方式已并入 target.mode (LIST/IMPORT/CONDITION)
     */
    @Deprecated
    private Integer type;

    /**
     * 任务阶段：1=验证，2=灰度，3=发布
     */
    private Integer phase;

    /**
     * 升级活动ID
     */
    private Long activityId;

    /**
     * 目标定义（结构化JSON），支持三种模式：
     * - LIST: 手动选择车辆列表，格式 {"mode":"LIST","vins":["VIN001","VIN002"]}
     * - IMPORT: 文件导入车辆，格式 {"mode":"IMPORT","vins":[...]} 或 {"mode":"IMPORT","fileId":"xxx"}
     * - CONDITION: 条件规则自动匹配，格式 {"mode":"CONDITION","logic":"AND","conditions":[{"field":"baselineCode","operator":"=","value":"BL001"}]}
     */
    private String target;

    /**
     * 任务开始时间
     */
    private Date startTime;

    /**
     * 任务结束时间
     */
    private Date endTime;

    /**
     * 任务发布时间
     */
    private Date releaseTime;

    /**
     * 通知类型（多选）：1 手机
     */
    private String noticeType;

    /**
     * 升级模式：1=普通，2=强制，3=预约静默，4=远程静默，5=工厂
     */
    private Integer upgradeMode;

    /**
     * 升级模式参数
     */
    private String upgradeModeArg;

    /**
     * 匹配条件列表（动态）
     */
    private List<TaskRestrictionMpt> restrictions;

    /**
     * 安装条件列表（动态）
     */
    private List<TaskInstallConditionMpt> installConditions;

    /**
     * 策略列表（动态）
     */
    private List<TaskStrategyMpt> strategies;

    /**
     * 任务状态：1=草稿，2=待审批，3=已审批，4=已驳回，5=已排程，6=已发布，7=已暂停，8=已完成，9=已取消，10=已取代
     */
    private Integer state;

    /**
     * 创建时间
     */
    private Date createTime;

}
