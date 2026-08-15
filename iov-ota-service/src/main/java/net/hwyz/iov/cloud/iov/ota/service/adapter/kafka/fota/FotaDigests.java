package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * FOTA 消息摘要工具（CR-014 §4.2/§8）
 *
 * <p>Envelope raw bytes SHA-256，用于 Inbox 幂等与 Outbox 字节一致性。
 *
 * @author hwyz_leo
 */
public final class FotaDigests {

    private FotaDigests() {
    }

    public static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
