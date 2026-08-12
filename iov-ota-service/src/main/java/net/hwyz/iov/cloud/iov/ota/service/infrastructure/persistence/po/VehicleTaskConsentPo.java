package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.util.Date;

/**
 * 车辆任务授权凭据 PO（CR-012 §3、§5.3）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_task_consent")
public class VehicleTaskConsentPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("vehicle_task_id")
    private Long vehicleTaskId;

    @TableField("consent_receipt_id")
    private String consentReceiptId;

    @TableField("terms_id")
    private Long termsId;

    @TableField("terms_hash")
    private String termsHash;

    @TableField("consent_scope_digest")
    private String consentScopeDigest;

    @TableField("consent_state")
    private String consentState;

    @TableField("accepted")
    private Integer accepted;

    @TableField("effective_state")
    private String effectiveState;

    @TableField("revoked_time")
    private Date revokedTime;

    @TableField("reconsent_required")
    private Integer reconsentRequired;
}
