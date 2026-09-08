package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

/**
 * 规范软件单元（CR-019 §4.1）
 *
 * <p>统一领域模型：同 ECU+Target+Slot 唯一，多 Slot 时恰好一个 active。
 * legacy SINGLE_IMAGE 规范化为 softwareTargetCode=ECU_IMAGE。
 *
 * @param softwareTargetCode 软件目标编码（稳定逻辑目标，非软件零件号）
 * @param softwarePartNumber 软件零件号
 * @param swVersion          软件版本
 * @param slot               运行槽位（可空；空=非 A/B 或缺省当前镜像）
 * @param active             是否活动槽
 * @param digest             软件单元内容摘要（可空）
 * @author hwyz_leo
 */
public record CanonicalSoftwareUnit(
        String softwareTargetCode,
        String softwarePartNumber,
        String swVersion,
        String slot,
        boolean active,
        String digest) {

    /** legacy SINGLE_IMAGE 统一目标编码 */
    public static final String LEGACY_TARGET = "ECU_IMAGE";

    /**
     * 是否 legacy 规范化单元（SINGLE_IMAGE → ECU_IMAGE）。
     */
    public boolean isLegacyImage() {
        return LEGACY_TARGET.equals(softwareTargetCode);
    }
}
