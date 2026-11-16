package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * 车辆任务包快照 PO（CR-012 §3、§5.4）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_task_package")
public class VehicleTaskPackagePo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("vehicle_task_id")
    private Long vehicleTaskId;

    @TableField("package_id")
    private String packageId;

    @TableField("package_revision")
    private String packageRevision;

    @TableField("etag")
    private String etag;

    @TableField("download_state")
    private String downloadState;

    @TableField("verify_state")
    private String verifyState;

    /** 最近一次下载授权返回的 current_offset_bytes（CR-026） */
    @TableField("grant_offset")
    private Long grantOffset;

    /** 最近一次授权 resume_allowed（CR-026） */
    @TableField("grant_resume_allowed")
    private Boolean grantResumeAllowed;

    /** 授权时的对象ETag（CR-026） */
    @TableField("grant_etag")
    private String grantEtag;

    /** 授权时的包版本号（CR-026） */
    @TableField("grant_package_revision")
    private String grantPackageRevision;

    /** 授权时间（CR-026） */
    @TableField("granted_at")
    private java.util.Date grantedAt;

    /** 授权凭证过期时间（CR-026） */
    @TableField("grant_expires_at")
    private java.util.Date grantExpiresAt;
}
