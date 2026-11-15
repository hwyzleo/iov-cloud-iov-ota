package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用事务管理器（无真实数据源）。
 *
 * <p>忠实模拟两件事，用于回归 2026-11-15 实车问题：
 * <ul>
 *   <li>共享事务被内层标记 rollback-only 后，提交阶段抛 {@link UnexpectedRollbackException}；</li>
 *   <li>{@code REQUIRES_NEW} 开启独立事务，不受外层已回滚/rollback-only 状态影响。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
final class FakeTransactionManager implements PlatformTransactionManager {

    private final List<Integer> propagations = new ArrayList<>();
    private final List<Integer> commits = new ArrayList<>();
    private int rollbacks;
    private boolean rollbackOnly;

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        int propagation = definition == null
                ? TransactionDefinition.PROPAGATION_REQUIRED
                : definition.getPropagationBehavior();
        propagations.add(propagation);
        if (propagation == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
            // 独立新事务：不继承外层的 rollback-only 标记
            rollbackOnly = false;
        }
        return new Status();
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        if (rollbackOnly) {
            rollbackOnly = false;
            throw new UnexpectedRollbackException(
                    "Transaction rolled back because it has been marked as rollback-only");
        }
        commits.add(propagations.size());
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        rollbacks++;
        rollbackOnly = false;
    }

    /** 模拟内层 {@code @Transactional} 方法抛异常后把共享事务标记 rollback-only。 */
    void markSharedTransactionRollbackOnly() {
        this.rollbackOnly = true;
    }

    int commitCount() {
        return commits.size();
    }

    int rollbackCount() {
        return rollbacks;
    }

    boolean usedRequiresNew() {
        return propagations.contains(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private final class Status implements TransactionStatus {

        private boolean completed;

        @Override
        public boolean isNewTransaction() {
            return true;
        }

        @Override
        public boolean hasSavepoint() {
            return false;
        }

        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public void flush() {
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public Object createSavepoint() throws TransactionException {
            throw new UnsupportedOperationException("测试事务管理器不支持保存点");
        }

        @Override
        public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        }

        @Override
        public void releaseSavepoint(Object savepoint) throws TransactionException {
        }
    }
}
