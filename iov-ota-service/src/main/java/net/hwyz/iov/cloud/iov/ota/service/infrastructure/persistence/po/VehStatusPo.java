package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po;

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
 * 车辆状态表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-16
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_status")
public class VehStatusPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 最后上报时间
     */
    @TableField("report_time")
    private Date reportTime;

    /**
     * 最后基线代码
     */
    @TableField("baseline_code")
    private String baselineCode;

    /**
     * 最后基线是否对齐
     */
    @TableField("baseline_alignment")
    private Boolean baselineAlignment;

    /**
     * 最后设备信息
     */
    @TableField("device_info")
    private String deviceInfo;

    /**
     * 最后升级活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 最后升级任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 最后升级配置字
     */
    @TableField("config_word")
    private String configWord;

    /**
     * 最后OTA Master版本
     */
    @TableField("master_version")
    private String masterVersion;
}
