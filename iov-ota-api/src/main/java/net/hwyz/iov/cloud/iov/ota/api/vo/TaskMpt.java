package net.hwyz.iov.cloud.iov.ota.api.contract;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.web.domain.BaseRequest;

import java.util.Date;

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
     * 任务类型：1=普通任务，2=快速任务
     */
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
     * 升级对象，普通任务时为文件代码，快速任务时为VIN
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
     * 保持驻车(P档)
     */
    private Boolean keepInPark;

    /**
     * 不在充电
     */
    private Boolean notCharging;

    /**
     * 不对外供电
     */
    private Boolean noExternalPower;

    /**
     * 车窗、天窗、车门及尾门关闭
     */
    private Boolean allClosed;

    /**
     * 高压电量
     */
    private Integer hvSoc;

    /**
     * 低压电量
     */
    private Integer lvSoc;

    /**
     * 是否影响车辆操作
     */
    private Boolean impactVehicleOperation;

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
     * 适配主体
     */
    private Integer adaptiveSubject;

    /**
     * 比较基准是否兼容
     */
    private Boolean comparisonCriteria;

    /**
     * 排除基线
     */
    private String excludedBaseline;

    /**
     * 基线拉齐
     */
    private Boolean baselineUnification;

    /**
     * 刷写次数
     */
    private Integer flashCount;

    /**
     * 是否回滚
     */
    private Boolean rollback;

    /**
     * 任务状态：1=待提交，2=待审核，3=已审核，4=未通过，5=已发布，6=已暂停，7=已结束，8=已取消
     */
    private Integer state;

    /**
     * 创建时间
     */
    private Date createTime;

}
