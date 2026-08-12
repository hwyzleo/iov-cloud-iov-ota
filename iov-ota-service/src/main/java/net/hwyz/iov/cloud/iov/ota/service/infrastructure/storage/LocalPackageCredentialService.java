package net.hwyz.iov.cloud.iov.ota.service.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.PackageCredentialService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 包下载凭证签发本地占位实现（CR-012 §5.4）
 *
 * <p>TODO: 接入 OSS/CDN 进行预签名凭证签发。当前返回模拟预签名 URL。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class LocalPackageCredentialService implements PackageCredentialService {

    @Override
    public PackageCredential signDownloadCredential(String packageId, String packageRevision, String etag,
                                                    long offset, Duration ttl) {
        // TODO: 接入 OSS/CDN，生成真正的预签名下载 URL
        String presignedUrl = "https://ota-oss.placeholder/" + packageId
                + "?revision=" + packageRevision
                + "&etag=" + etag
                + "&offset=" + offset
                + "&token=" + UUID.randomUUID();
        String credentialToken = "cred-" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(ttl);

        log.debug("本地占位签发包下载凭证，包[{}]，偏移[{}]", packageId, offset);
        return new PackageCredential(presignedUrl, credentialToken, expiresAt, offset, offset > 0 && etag == null);
    }
}
