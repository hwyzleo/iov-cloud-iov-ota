package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.Date;

/**
 * tb_approved_sw_manifest 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_approved_sw_manifest")
public class ApprovedSwManifestPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 快照编码
     */
    @TableField("manifest_code")
    private String manifestCode;

    /**
     * 升级活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * SWIN代码
     */
    @TableField("swin_code")
    private String swinCode;

    /**
     * RXSWIN值
     */
    @TableField("rxswin_value")
    private String rxswinValue;

    /**
     * 快照状态
     */
    @TableField("manifest_status")
    private String manifestStatus;

    /**
     * 批准时间
     */
    @TableField("approve_time")
    private Date approveTime;

}
