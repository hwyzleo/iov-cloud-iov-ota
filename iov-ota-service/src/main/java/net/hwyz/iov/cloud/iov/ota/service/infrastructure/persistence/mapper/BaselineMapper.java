package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselinePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Baseline DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaselineMapper extends BaseMapper<BaselinePo> {

    /**
     * 恢复软删除记录并更新字段（绕过 row_valid 过滤）
     */
    @Update("UPDATE tb_baseline SET row_valid = 1, name = #{po.name}, anchor_type = #{po.anchorType}, " +
            "anchor_code = #{po.anchorCode}, baseline_version = #{po.baselineVersion}, " +
            "baseline_status = #{po.baselineStatus}, source = #{po.source}, sync_time = #{po.syncTime}, " +
            "modify_time = #{po.modifyTime} WHERE baseline_code = #{po.baselineCode}")
    int restoreAndUpdate(@Param("po") BaselinePo po);
}
