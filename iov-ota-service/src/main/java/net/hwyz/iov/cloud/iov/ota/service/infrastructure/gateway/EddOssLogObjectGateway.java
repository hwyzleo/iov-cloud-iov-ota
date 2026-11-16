package net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.oss.api.service.LogObjectService;
import net.hwyz.iov.cloud.edd.oss.api.vo.request.GenerateLogObjectUploadRequest;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.LogObjectDownloadUrlResponse;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.LogObjectResponse;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.LogObjectUploadResponse;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.LogObjectOperationException;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.LogObjectStorageGateway;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EDD-OSS 日志对象网关适配器（IOV-OTA-DSN-CR-026 §7.3）。
 *
 * <p>把 EDD-OSS 的稳定错误标识映射为 IOV-OTA 403011/403012/403013/403016：
 * 确定性失败不自动重试，存储暂不可用按可恢复故障处理；fallback 与超时一律视为可恢复。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EddOssLogObjectGateway implements LogObjectStorageGateway {

    /** EDD-OSS 业务类型：OTA 升级日志 */
    private static final String BUSINESS_TYPE_OTA_UPGRADE_LOG = "OTA_UPGRADE_LOG";
    /** 响应体中的 EDD-OSS 错误码 */
    private static final Pattern OSS_CODE = Pattern.compile("\"code\"\\s*:\\s*\"(OSS-\\d+)\"");

    private final LogObjectService logObjectService;

    @Override
    public Authorization authorize(AuthorizationRequest request) {
        GenerateLogObjectUploadRequest upstream = GenerateLogObjectUploadRequest.builder()
                .businessType(request.businessType() == null ? BUSINESS_TYPE_OTA_UPGRADE_LOG : request.businessType())
                .businessId(request.businessId())
                .businessSubId(request.businessSubId())
                .requestId(request.requestId())
                .fileName(request.fileName())
                .contentType(request.contentType())
                .sizeBytes(request.sizeBytes())
                .sha256Hex(request.sha256Hex())
                .privacyLevel(request.privacyLevel())
                .ttlSeconds(request.ttlSeconds())
                .build();
        try {
            LogObjectUploadResponse response = logObjectService.generateUpload(upstream);
            if (response == null || response.getObjectId() == null || response.getObjectKey() == null) {
                throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STORAGE_UNAVAILABLE,
                        "日志对象授权响应不完整", true);
            }
            return new Authorization(response.getObjectId(), response.getObjectKey(), response.getUploadUrl(),
                    response.getHeaders(), toInstant(response.getExpiresAt()), response.getMaxSizeBytes());
        } catch (RuntimeException e) {
            throw translate(e, "申请日志对象上传授权失败");
        }
    }

    @Override
    public Fact complete(String objectId) {
        try {
            return toFact(logObjectService.completeUpload(objectId));
        } catch (RuntimeException e) {
            throw translate(e, "日志对象上传核验失败");
        }
    }

    @Override
    public Fact detail(String objectId) {
        try {
            return toFact(logObjectService.getObject(objectId));
        } catch (RuntimeException e) {
            throw translate(e, "查询日志对象登记事实失败");
        }
    }

    @Override
    public DownloadUrl downloadUrl(String objectId) {
        try {
            LogObjectDownloadUrlResponse response = logObjectService.generateDownloadUrl(objectId);
            return new DownloadUrl(response.getObjectId(), response.getDownloadUrl(),
                    toInstant(response.getExpiresAt()));
        } catch (RuntimeException e) {
            throw translate(e, "签发日志对象下载地址失败");
        }
    }

    private static Fact toFact(LogObjectResponse response) {
        if (response == null) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_OBJECT_NOT_FOUND,
                    "日志对象登记不存在");
        }
        return new Fact(response.getObjectId(), response.getObjectKey(), response.getUploadState(),
                response.getSizeBytes(), response.getActualSizeBytes(), response.getSha256Hex(),
                response.getActualSha256Hex(), response.getEtag(), response.getFailureReason(),
                toInstant(response.getAuthorizedAt()), toInstant(response.getUploadedAt()),
                toInstant(response.getReceivedAt()));
    }

    /**
     * 异常映射：优先解析 EDD-OSS 稳定错误标识，其次按 HTTP 状态判定。
     */
    private RuntimeException translate(RuntimeException exception, String operation) {
        if (exception instanceof LogObjectOperationException logException) {
            return logException;
        }
        if (exception instanceof FeignException feignException) {
            int status = feignException.status();
            String ossCode = extractOssCode(feignException);
            log.warn("{}：status[{}] ossCode[{}]", operation, status, ossCode);
            if (ossCode != null) {
                switch (ossCode) {
                    case "OSS-811019":
                        return new LogObjectOperationException(
                                LogObjectOperationException.ERROR_CODE_OBJECT_NOT_FOUND, operation + "：对象不存在");
                    case "OSS-811020":
                    case "OSS-811021":
                    case "OSS-811025":
                        return new LogObjectOperationException(
                                LogObjectOperationException.ERROR_CODE_OBJECT_MISMATCH, operation + "：对象不符");
                    case "OSS-811018":
                    case "OSS-811022":
                    case "OSS-811023":
                    case "OSS-811024":
                        return new LogObjectOperationException(
                                LogObjectOperationException.ERROR_CODE_STATE_CONFLICT, operation + "：状态或幂等冲突");
                    case "OSS-811017":
                        return new LogObjectOperationException(
                                LogObjectOperationException.ERROR_CODE_STORAGE_UNAVAILABLE, operation + "：存储暂不可用", true);
                    default:
                        break;
                }
            }
            if (status == 404) {
                return new LogObjectOperationException(
                        LogObjectOperationException.ERROR_CODE_OBJECT_NOT_FOUND, operation + "：登记不存在");
            }
            if (status == 422) {
                return new LogObjectOperationException(
                        LogObjectOperationException.ERROR_CODE_OBJECT_MISMATCH, operation + "：对象不符");
            }
            if (status == 409 || status == 403 || status == 400 || status == 401) {
                return new LogObjectOperationException(
                        LogObjectOperationException.ERROR_CODE_STATE_CONFLICT, operation + "：状态或请求冲突");
            }
            if (status >= 500 || status <= 0) {
                return new LogObjectOperationException(
                        LogObjectOperationException.ERROR_CODE_STORAGE_UNAVAILABLE, operation + "：存储暂不可用", true);
            }
            return new LogObjectOperationException(
                    LogObjectOperationException.ERROR_CODE_STORAGE_UNAVAILABLE, operation + "：未知故障", true);
        }
        // fallback（IllegalStateException）与连接/读取超时：可恢复基础设施故障
        return new LogObjectOperationException(
                LogObjectOperationException.ERROR_CODE_STORAGE_UNAVAILABLE, operation + "：" + exception.getMessage(), true);
    }

    private static String extractOssCode(FeignException exception) {
        String body = exception.contentUTF8();
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher matcher = OSS_CODE.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Instant toInstant(Long epochMilli) {
        return epochMilli == null ? null : Instant.ofEpochMilli(epochMilli);
    }
}
