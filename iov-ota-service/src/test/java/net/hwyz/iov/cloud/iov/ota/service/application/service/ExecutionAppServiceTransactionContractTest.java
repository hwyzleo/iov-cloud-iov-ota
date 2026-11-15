package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 申请安装许可的事务契约测试（2026-11-15 实车问题回归）。
 *
 * <p>{@code requestInstall} 的前置守卫失败是确定性业务拒绝，调用方会捕获
 * {@link ExecutionStateException} 并回 {@code allowed=false + deny_reason} 的 RESPONSE。
 * 若按默认规则回滚，加入同一物理事务的 Kafka 入站流水线会被标记 rollback-only，
 * 提交阶段抛 {@code UnexpectedRollbackException}，拒绝响应被一起回滚、offset 无限重投直至隔离。
 *
 * @author hwyz_leo
 */
@DisplayName("ExecutionAppService - 安装许可业务拒绝不得回滚外层事务")
class ExecutionAppServiceTransactionContractTest {

    @Test
    @DisplayName("requestInstall 必须声明 noRollbackFor = ExecutionStateException")
    void requestInstall_must_not_rollback_on_business_rejection() throws NoSuchMethodException {
        Transactional tx = ExecutionAppService.class
                .getMethod("requestInstall", ExecutionCreateCmd.class)
                .getAnnotation(Transactional.class);

        assertTrue(tx != null, "requestInstall 必须声明 @Transactional");
        assertTrue(List.of(tx.noRollbackFor()).contains(ExecutionStateException.class),
                "noRollbackFor 必须包含 ExecutionStateException，否则业务拒绝会污染外层事务并回滚拒绝响应");
    }
}
