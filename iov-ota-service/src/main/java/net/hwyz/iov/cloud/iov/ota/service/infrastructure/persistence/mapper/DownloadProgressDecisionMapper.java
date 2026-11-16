package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 下载进度业务幂等处置 Mapper（复用 tb_idempotency_record，CR-026 §6.2/§7.1）。
 *
 * <p>PAR-PROTO-DSN-CR-011 冻结「REJECTED/CONFLICT 只保留 progressId 首次处置用于幂等回放，
 * 不占用进度第二唯一键」，因此首次处置不写入进度历史表，而写入平台幂等记录。</p>
 *
 * @author hwyz_leo
 */
@Mapper
public interface DownloadProgressDecisionMapper {

    /**
     * 按幂等作用域 + progressId 读取首次处置。
     */
    @Select("SELECT request_digest, response_snapshot FROM tb_idempotency_record "
            + "WHERE operation_scope = #{scope} AND idempotency_key = #{progressId} LIMIT 1")
    java.util.Map<String, Object> selectDecision(@Param("scope") String scope,
                                                 @Param("progressId") String progressId);

    /**
     * 写入首次处置（并发重复由唯一键裁决）。
     */
    @Insert("INSERT INTO tb_idempotency_record (operation_scope, idempotency_key, request_digest, "
            + "response_snapshot, vin, create_time, modify_time) VALUES (#{scope}, #{progressId}, "
            + "#{digest}, #{snapshot}, #{vin}, NOW(3), NOW(3))")
    int insertDecision(@Param("scope") String scope, @Param("progressId") String progressId,
                       @Param("digest") String digest, @Param("snapshot") String snapshot,
                       @Param("vin") String vin);

    /**
     * 把进度快照推进到更高序号（条件更新，防并发回退）。
     */
    @Update("UPDATE tb_package_download_progress SET latest_progress_id = #{latestProgressId}, "
            + "latest_progress_seq = #{latestProgressSeq}, download_state = #{downloadState}, "
            + "downloaded_bytes = #{downloadedBytes}, total_bytes = #{totalBytes}, "
            + "resume_offset = #{resumeOffset}, progress_percent = #{progressPercent}, etag = #{etag}, "
            + "error_code = #{errorCode}, failure_reason = #{failureReason}, "
            + "source_occurred_at = #{sourceOccurredAt}, received_at = #{receivedAt}, "
            + "modify_time = NOW(3), row_version = row_version + 1 "
            + "WHERE vehicle_task_id = #{vehicleTaskId} AND package_id = #{packageId} "
            + "AND download_attempt_no = #{downloadAttemptNo} AND latest_progress_seq < #{latestProgressSeq}")
    int advanceSnapshot(@Param("vehicleTaskId") Long vehicleTaskId, @Param("packageId") String packageId,
                        @Param("downloadAttemptNo") Integer downloadAttemptNo,
                        @Param("latestProgressId") String latestProgressId,
                        @Param("latestProgressSeq") Long latestProgressSeq,
                        @Param("downloadState") String downloadState,
                        @Param("downloadedBytes") Long downloadedBytes,
                        @Param("totalBytes") Long totalBytes,
                        @Param("resumeOffset") Long resumeOffset,
                        @Param("progressPercent") java.math.BigDecimal progressPercent,
                        @Param("etag") String etag,
                        @Param("errorCode") String errorCode,
                        @Param("failureReason") String failureReason,
                        @Param("sourceOccurredAt") java.util.Date sourceOccurredAt,
                        @Param("receivedAt") java.util.Date receivedAt);
}
