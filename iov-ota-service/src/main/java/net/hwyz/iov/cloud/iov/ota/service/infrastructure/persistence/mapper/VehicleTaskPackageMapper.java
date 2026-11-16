package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskPackagePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 车辆任务包快照 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleTaskPackageMapper extends BaseMapper<VehicleTaskPackagePo> {

    @Select("SELECT * FROM tb_vehicle_task_package WHERE vehicle_task_id = #{vehicleTaskId} AND row_valid = 1")
    List<VehicleTaskPackagePo> selectByVehicleTaskId(@Param("vehicleTaskId") Long vehicleTaskId);

    /**
     * 记录最近一次下载授权证据（PAR-PROTO-DSN-CR-011 冻结的 RESET_OFFSET 裁决依据）。
     *
     * @param vehicleTaskId      车辆任务ID
     * @param packageId          包ID
     * @param grantOffset        授权返回的 current_offset_bytes
     * @param resumeAllowed      授权响应 resume_allowed
     * @param etag               授权时对象ETag
     * @param packageRevision    授权时包版本号
     * @param expiresAt          授权凭证过期时间
     * @return 影响行数
     */
    @Update("UPDATE tb_vehicle_task_package SET grant_offset = #{grantOffset}, "
            + "grant_resume_allowed = #{resumeAllowed}, grant_etag = #{etag}, "
            + "grant_package_revision = #{packageRevision}, granted_at = NOW(3), "
            + "grant_expires_at = #{expiresAt}, modify_time = NOW(3), row_version = row_version + 1 "
            + "WHERE vehicle_task_id = #{vehicleTaskId} AND package_id = #{packageId} AND row_valid = 1")
    int updateGrantEvidence(@Param("vehicleTaskId") Long vehicleTaskId, @Param("packageId") String packageId,
                            @Param("grantOffset") Long grantOffset, @Param("resumeAllowed") Boolean resumeAllowed,
                            @Param("etag") String etag, @Param("packageRevision") String packageRevision,
                            @Param("expiresAt") java.util.Date expiresAt);
}
