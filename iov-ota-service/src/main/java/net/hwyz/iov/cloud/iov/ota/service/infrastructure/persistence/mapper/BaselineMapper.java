package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselinePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * Baseline DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaselineMapper extends BaseMapper<BaselinePo> {

}
