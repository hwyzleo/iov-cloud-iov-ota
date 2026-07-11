package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * tb_swin_managed_system 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_swin_managed_system")
public class SwinManagedSystemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * SWIN代码
     */
    @TableField("swin_code")
    private String swinCode;

    /**
     * 车载节点代码
     */
    @TableField("vehicle_node_code")
    private String vehicleNodeCode;

    /**
     * 是否与型式批准相关
     */
    @TableField("is_type_approval_relevant")
    private Boolean isTypeApprovalRelevant;

    /**
     * 已批准的软件基线代码
     */
    @TableField("approved_software_baseline")
    private String approvedSoftwareBaseline;

}
