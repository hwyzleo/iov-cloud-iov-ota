package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 乐观锁冲突异常（CR-015 §5）
 * <p>schedule 等更新需携带 rowVersion；版本不匹配（已被他人修改）时抛出，前端需刷新重试。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
public class OptimisticLockException extends OtaBaseException {

    private static final int ERROR_CODE = 412001;

    public OptimisticLockException(String message) {
        super(ERROR_CODE);
        log.warn("乐观锁冲突: {}", message);
    }
}
