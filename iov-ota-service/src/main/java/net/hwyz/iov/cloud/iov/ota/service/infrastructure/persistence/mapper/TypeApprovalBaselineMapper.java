package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TypeApprovalBaselinePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * TypeApprovalBaseline Mapper
 *
 * @author hwyz_leo
 */
@Mapper
public interface TypeApprovalBaselineMapper extends BaseMapper<TypeApprovalBaselinePo> {

    /**
     * 恢复软删除记录并更新字段（绕过 row_valid 过滤）
     */
    @Update("UPDATE tb_type_approval_baseline SET row_valid = 1, swin_code = #{po.swinCode}, " +
            "anchor_type = #{po.anchorType}, anchor_code = #{po.anchorCode}, status = #{po.status}, " +
            "projection_digest = #{po.projectionDigest}, effective_from = #{po.effectiveFrom}, " +
            "source_baseline_scope = #{po.sourceBaselineScope}, up_version = #{po.upVersion}, " +
            "sync_time = #{po.syncTime}, modify_time = #{po.modifyTime} " +
            "WHERE ta_baseline_code = #{po.taBaselineCode}")
    int restoreAndUpdate(@Param("po") TypeApprovalBaselinePo po);
}
