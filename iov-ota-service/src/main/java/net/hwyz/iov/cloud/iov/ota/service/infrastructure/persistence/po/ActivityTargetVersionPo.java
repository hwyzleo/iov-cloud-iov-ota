package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * tb_activity_target_version 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_activity_target_version")
public class ActivityTargetVersionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 升级活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 基线代码
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
     * 目标软件内部版本
     */
    @TableField("target_software_build_ver")
    private String targetSoftwareBuildVer;

}
