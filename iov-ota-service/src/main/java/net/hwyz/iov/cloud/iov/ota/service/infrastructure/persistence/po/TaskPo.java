package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 升级任务表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task")
public class TaskPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    @TableField("name")
    private String name;

    /**
     * 任务类型：1=普通任务，2=快速任务
     */
    @TableField("type")
    private Integer type;

    /**
     * 任务阶段：1=验证，2=灰度，3=发布
     */
    @TableField("phase")
    private Integer phase;

    /**
     * 升级活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 升级对象，普通任务时为文件代码，快速任务时为VIN
     */
    @TableField("target")
    private String target;

    /**
     * 任务开始时间
     */
    @TableField("start_time")
    private Date startTime;

    /**
     * 任务结束时间
     */
    @TableField("end_time")
    private Date endTime;

    /**
     * 任务发布时间（计划发布时间）
     */
    @TableField("release_time")
    private Date releaseTime;

    /**
     * 实际发布时间（发布事务成功时间）
     */
    @TableField("actual_release_time")
    private Date actualReleaseTime;

    /**
     * 最近一次到点发布失败摘要
     */
    @TableField("last_schedule_error")
    private String lastScheduleError;

    /**
     * 通知类型（多选）：1 手机
     */
    @TableField("notice_type")
    private String noticeType;

    /**
     * 升级模式：1=普通，2=强制，3=预约静默，4=远程静默，5=工厂
     */
    @TableField("upgrade_mode")
    private Integer upgradeMode;

    /**
     * 升级模式参数
     */
    @TableField("upgrade_mode_arg")
    private String upgradeModeArg;

    /**
     * 任务状态：1=待提交，2=待审核，3=已审核，4=未通过，5=已发布，6=已暂停，7=已结束，8=已取消
     */
    @TableField("state")
    private Integer state;

    /**
     * 暂停前的状态
     */
    @TableField("state_before_pause")
    private Integer stateBeforePause;
}
