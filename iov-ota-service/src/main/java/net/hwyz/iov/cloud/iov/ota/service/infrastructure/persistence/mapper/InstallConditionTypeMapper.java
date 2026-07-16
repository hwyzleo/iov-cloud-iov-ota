package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.InstallConditionTypePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 安装条件类型 Mapper 接口
 */
@Mapper
public interface InstallConditionTypeMapper extends BaseMapper<InstallConditionTypePo> {
    
}
