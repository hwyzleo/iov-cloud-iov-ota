package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageDownloadProgressEventPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 包下载进度不可变历史 Mapper（CR-026 §6.2）。
 *
 * @author hwyz_leo
 */
@Mapper
public interface PackageDownloadProgressEventMapper extends BaseMapper<PackageDownloadProgressEventPo> {
}
