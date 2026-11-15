package net.hwyz.iov.cloud.iov.ota.service.domain.gateway;

/**
 * 包清单摘要校验指标网关（CR-024 §4.28.6）。
 *
 * <p>领域层只声明语义，落地实现由基础设施层提供；两类事件必须分开计数：
 * 「云端未冻结摘要而跳过校验」与「车端摘要与云端重算值不一致」。</p>
 *
 * @author hwyz_leo
 */
public interface ManifestDigestMetrics {

    /**
     * 云端尚未冻结包清单摘要，本次跳过校验（fail-open）。
     */
    void unverifiable();

    /**
     * 车端摘要与云端重算值不一致，本次拒绝签发许可。
     */
    void mismatch();
}
