package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * 幂等记录 PO（CR-012 §4）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_idempotency_record")
public class IdempotencyRecordPo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("operation_scope")
    private String operationScope;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("request_digest")
    private String requestDigest;

    @TableField("response_snapshot")
    private String responseSnapshot;

    @TableField("vin")
    private String vin;

    @TableField("create_time")
    private Date createTime;

    @TableField("modify_time")
    private Date modifyTime;
}
