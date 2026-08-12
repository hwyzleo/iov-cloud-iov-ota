package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.OutboxMessagePo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 事务性 Outbox 消息 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface OutboxMessageMapper {

    @Insert("INSERT INTO tb_outbox_message (aggregate_type, aggregate_id, event_type, payload_json, occurred_at, status, retry_count, create_time, modify_time) "
            + "VALUES (#{aggregateType}, #{aggregateId}, #{eventType}, #{payloadJson}, #{occurredAt}, 'PENDING', 0, now(), now())")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OutboxMessagePo po);

    @Select("SELECT * FROM tb_outbox_message WHERE id = #{id}")
    OutboxMessagePo selectById(@Param("id") Long id);

    @Select("SELECT * FROM tb_outbox_message WHERE status = 'PENDING' ORDER BY occurred_at LIMIT #{limit}")
    List<OutboxMessagePo> selectPending(@Param("limit") int limit);

    @Update("UPDATE tb_outbox_message SET status = 'PUBLISHED', published_at = now(), modify_time = now() WHERE id = #{id}")
    int markPublished(@Param("id") Long id);

    @Update("UPDATE tb_outbox_message SET status = 'FAILED', retry_count = retry_count + 1, last_error = #{reason}, modify_time = now() WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("reason") String reason);
}
