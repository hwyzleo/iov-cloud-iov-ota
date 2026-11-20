package net.hwyz.iov.cloud.iov.ota.service.domain.gateway;

/**
 * 任务下发期包元数据门禁豁免指标网关（IOV-OTA-DSN-CR-027 §4.32.5）。
 *
 * <p>联调期显式关闭 {@code ota.task-check.package-metadata-gate.enabled} 后，
 * 包元数据不完整但仍下发任务快照属于「被豁免的放行」，必须与正常放行区分计数，
 * 避免豁免被静默使用。发布期与包登记期门禁不受该开关影响。</p>
 *
 * @author hwyz_leo
 */
public interface PackageMetadataGateMetrics {

    /**
     * 记录一次下发期包元数据门禁豁免。
     *
     * @param reasonCodes 被豁免的原因码集合（形如 {@code packageId:PACKAGE_SHAPE...;...}）
     */
    void bypassed(String reasonCodes);
}
