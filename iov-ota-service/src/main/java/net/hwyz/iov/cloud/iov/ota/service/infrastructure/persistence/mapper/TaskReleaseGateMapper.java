package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskReleaseGatePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 多任务放行门禁 Mapper 接口（CR-015）
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskReleaseGateMapper extends BaseMapper<TaskReleaseGatePo> {

}
