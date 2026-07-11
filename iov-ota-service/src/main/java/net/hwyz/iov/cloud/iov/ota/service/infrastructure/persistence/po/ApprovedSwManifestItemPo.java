package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * tb_approved_sw_manifest_item 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_approved_sw_manifest_item")
public class ApprovedSwManifestItemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 快照ID
     */
    @TableField("manifest_id")
    private Long manifestId;

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

}
