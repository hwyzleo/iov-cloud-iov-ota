package net.hwyz.iov.cloud.iov.ota.service.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.FotaV2ErrorCode;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.FotaV2Exception;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.IdempotencyRecordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.IdempotencyRecordPo;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 幂等服务（CR-012 §4、US-073）
 *
 * <p>写操作锁定 operationScope + idempotencyKey：
 * 相同幂等键、相同请求摘要返回原响应；相同键、不同摘要返回 OTA-AUTH-006 冲突。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordMapper idempotencyRecordMapper;

    /**
     * 幂等执行写操作。
     *
     * @param operationScope 操作作用域（如 INSTALL_EXECUTION）
     * @param idempotencyKey 幂等键
     * @param requestDigest  请求摘要
     * @param vin            车架号
     * @param executor       实际业务执行
     * @param <T>            响应类型
     * @return 业务响应
     */
    public <T> T execute(String operationScope, String idempotencyKey, String requestDigest,
                         String vin, Supplier<T> executor) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return executor.get();
        }

        IdempotencyRecordPo existing = idempotencyRecordMapper.selectByScopeAndKey(operationScope, idempotencyKey);
        if (existing != null) {
            if (existing.getRequestDigest().equals(requestDigest)) {
                log.info("幂等命中，返回原响应：scope[{}] key[{}]", operationScope, idempotencyKey);
                return deserialize(existing.getResponseSnapshot());
            }
            throw new FotaV2Exception(FotaV2ErrorCode.AUTH_IDEMPOTENCY_CONFLICT,
                    "相同幂等键但请求摘要不同");
        }

        T result = executor.get();
        IdempotencyRecordPo po = IdempotencyRecordPo.builder()
                .operationScope(operationScope)
                .idempotencyKey(idempotencyKey)
                .requestDigest(requestDigest)
                .responseSnapshot(serialize(result))
                .vin(vin)
                .build();
        idempotencyRecordMapper.insert(po);
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return (T) json;
    }

    private <T> String serialize(T result) {
        return result != null ? String.valueOf(result) : null;
    }
}
