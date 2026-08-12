package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.IdempotencyRecordPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 幂等记录 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface IdempotencyRecordMapper {

    @Select("SELECT * FROM tb_idempotency_record WHERE operation_scope = #{operationScope} AND idempotency_key = #{idempotencyKey}")
    IdempotencyRecordPo selectByScopeAndKey(@Param("operationScope") String operationScope, @Param("idempotencyKey") String idempotencyKey);

    @Insert("INSERT INTO tb_idempotency_record (operation_scope, idempotency_key, request_digest, response_snapshot, vin, create_time, modify_time) "
            + "VALUES (#{operationScope}, #{idempotencyKey}, #{requestDigest}, #{responseSnapshot}, #{vin}, now(), now())")
    int insert(IdempotencyRecordPo po);

    @Update("UPDATE tb_idempotency_record SET response_snapshot = #{responseSnapshot}, modify_time = now() WHERE operation_scope = #{operationScope} AND idempotency_key = #{idempotencyKey}")
    int updateResponse(IdempotencyRecordPo po);
}
