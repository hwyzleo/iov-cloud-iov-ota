package net.hwyz.iov.cloud.iov.ota.api.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 升级任务排程（定时发布）请求
 *
 * <p>时间契约遵循 CR-021（§4.25）：绝对时间字段使用带 offset 的 ISO-8601 字符串
 * （推荐，如 {@code 2026-11-10T02:00:00.000Z} / {@code 2026-11-10T10:00:00+08:00}），
 * 历史客户端仍可发送 10 位 epoch 秒或 13 位 epoch 毫秒整数；由框架
 * {@code IovInstantDeserializer} 统一双读，服务端不再自行字符串解析或猜测时区。</p>
 *
 * <p>不带时区/offset 的字符串（如 {@code 2026-11-10T10:00:00}）会被明确拒绝，
 * 避免依赖服务器默认时区产生 8 小时偏移。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskScheduleRequest {

    /**
     * 计划发布时间（带 offset 的 ISO-8601 绝对时刻，或历史 epoch 秒/毫秒整数）
     *
     * <p>由框架 IovJavaTimeReadModule / IovInstantDeserializer 统一反序列化：
     * 10 位整数视为 epoch 秒、13 位视为 epoch 毫秒，拒绝空串、小数时间戳与无 offset 字符串。</p>
     */
    @NotNull(message = "计划发布时间不能为空")
    private Instant releaseTime;

    /**
     * 乐观锁版本（可选；为空时服务端取库内当前版本）
     */
    private Integer rowVersion;

}
