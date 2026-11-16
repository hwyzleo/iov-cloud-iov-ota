package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageDownloadProgressPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 包下载进度当前快照 Mapper（CR-026 §6.1）。
 *
 * @author hwyz_leo
 */
@Mapper
public interface PackageDownloadProgressMapper extends BaseMapper<PackageDownloadProgressPo> {

    /**
     * 按 (vehicleTaskId, packageId, downloadAttemptNo) 加悲观写锁读取当前快照。
     *
     * @param vehicleTaskId     车辆任务ID
     * @param packageId         包ID
     * @param downloadAttemptNo 下载尝试序号
     * @return 当前快照行（不存在为 null）
     */
    @Select("SELECT * FROM tb_package_download_progress "
            + "WHERE vehicle_task_id = #{vehicleTaskId} AND package_id = #{packageId} "
            + "AND download_attempt_no = #{downloadAttemptNo} LIMIT 1 FOR UPDATE")
    PackageDownloadProgressPo selectForUpdate(@Param("vehicleTaskId") Long vehicleTaskId,
                                              @Param("packageId") String packageId,
                                              @Param("downloadAttemptNo") Integer downloadAttemptNo);
}
