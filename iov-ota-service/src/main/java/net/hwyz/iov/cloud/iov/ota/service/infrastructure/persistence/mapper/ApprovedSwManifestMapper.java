package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * ApprovedSwManifest DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface ApprovedSwManifestMapper extends BaseMapper<ApprovedSwManifestPo> {

}
