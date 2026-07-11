package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskReportPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaskReport DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskReportMapper extends BaseMapper<TaskReportPo> {

}
