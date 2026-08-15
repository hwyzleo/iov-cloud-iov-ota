package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

/**
 * Protobuf → JSON 工具（CR-014 §4/§5）
 *
 * <p>仅用于将嵌套策略/事件等结构化为应用命令 DTO 的 JSON 承载字段，
 * 不用于 Envelope/payload 传输编解码（传输必须是完整 Envelope bytes，禁止 JSON fallback）。
 *
 * @author hwyz_leo
 */
public final class FotaJson {

    private FotaJson() {
    }

    /**
     * 将 protobuf 消息序列化为 JSON；空/缺失返回 null。
     */
    public static String toJson(MessageOrBuilder message) {
        if (message == null) {
            return null;
        }
        try {
            return JsonFormat.printer().includingDefaultValueFields().print(message);
        } catch (Exception e) {
            throw new IllegalStateException("Protobuf JSON 序列化失败: " + e.getMessage(), e);
        }
    }
}
