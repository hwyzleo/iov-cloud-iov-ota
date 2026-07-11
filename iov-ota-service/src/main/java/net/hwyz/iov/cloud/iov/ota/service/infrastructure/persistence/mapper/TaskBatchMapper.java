package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskBatchPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaskBatch DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskBatchMapper extends BaseMapper<TaskBatchPo> {

}
