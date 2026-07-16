package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskInstallConditionPo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务安装条件 Mapper 接口
 */
@Mapper
public interface TaskInstallConditionMapper extends BaseMapper<TaskInstallConditionPo> {

    @Delete("DELETE FROM tb_task_install_condition WHERE task_id = #{taskId}")
    void deleteByTaskId(Long taskId);
}
