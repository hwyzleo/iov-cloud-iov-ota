package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinDefinitionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * SwinDefinition DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface SwinDefinitionMapper extends BaseMapper<SwinDefinitionPo> {

    /**
     * 恢复软删除记录并更新字段（绕过 row_valid 过滤）
     */
    @Update("UPDATE tb_swin_definition SET row_valid = 1, scheme_code = #{po.schemeCode}, " +
            "type_ref_type = #{po.typeRefType}, type_ref_code = #{po.typeRefCode}, name = #{po.name}, " +
            "status = #{po.status}, sync_time = #{po.syncTime}, modify_time = #{po.modifyTime} " +
            "WHERE swin_code = #{po.swinCode}")
    int restoreAndUpdate(@Param("po") SwinDefinitionPo po);
}
