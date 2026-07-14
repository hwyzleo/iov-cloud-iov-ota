package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 活动升级对象表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-13
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_activity_upgrade_target")
public class ActivityUpgradeTargetPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 来源类型：0 手动，1 基线
     */
    @TableField("source_type")
    private Integer sourceType;

    /**
     * 基线代码（source_type=1时必填）
     */
    @TableField("baseline_code")
    private String baselineCode;

    /**
     * 车载节点代码
     */
    @TableField("vehicle_node_code")
    private String vehicleNodeCode;

    /**
     * 软件零件号
     */
    @TableField("part_code")
    private String partCode;

    /**
     * 软件内部版本ID（可空占位）
     */
    @TableField("software_build_version_id")
    private Long softwareBuildVersionId;

    /**
     * 是否关键版本（可空）
     */
    @TableField("critical")
    private Boolean critical;

    /**
     * 是否支持OTA（可空）
     */
    @TableField("ota")
    private Boolean ota;

    /**
     * 安装顺序号（原sort）
     */
    @TableField("install_seq")
    private Integer installSeq;

    /**
     * 并行组号（同组并行执行）
     */
    @TableField("parallel_group")
    private Integer parallelGroup;

    /**
     * 活动内分组号（0或空=独立，原version_group）
     */
    @TableField("group_no")
    private Integer groupNo;

    /**
     * 是否强制升级
     */
    @TableField("force_upgrade")
    private Boolean forceUpgrade;
}