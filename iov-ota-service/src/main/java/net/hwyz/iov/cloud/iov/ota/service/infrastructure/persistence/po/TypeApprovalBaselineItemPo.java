package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * tb_type_approval_baseline_item 数据对象
 * 型式批准基线明细投影表
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_type_approval_baseline_item")
public class TypeApprovalBaselineItemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * TA基线代码
     */
    @TableField("ta_baseline_code")
    private String taBaselineCode;

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
     * 批准版本
     */
    @TableField("approved_version")
    private String approvedVersion;

    /**
     * 溯源：来源基线代码
     */
    @TableField("source_baseline_code")
    private String sourceBaselineCode;

}
