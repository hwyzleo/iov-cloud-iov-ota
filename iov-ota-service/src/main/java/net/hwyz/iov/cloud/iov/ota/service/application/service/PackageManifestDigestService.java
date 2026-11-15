package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.VehicleTaskSnapshotSeal;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 包清单摘要规范化算法（CR-024 §4.28.2，canonicalization v1）。
 *
 * <p>与 §4.23.2 Canonical Digest 同构：先对输入确定性排序，再以「字段号 + 类型 + presence + 长度」
 * 的确定性编码拼装字节，最后 SHA-256 取小写十六进制。禁止第二套归约规则。</p>
 *
 * <p>纳入摘要的字段与下发给车端的 {@code PackageSummary} 对齐，但**排除 `sourceVersion`**：
 * 它来自该车已受理清单的当前版本，逐车不同且随清单上报变化，纳入后将无法在发布期冻结出
 * 一车一份之外的稳定摘要（CR-024 D1-A）。</p>
 *
 * <p>摘要空间按 {@link #CANONICALIZATION_VERSION} 隔离：不同版本摘要禁止互相比较，
 * 也不得以新版本重算覆盖存量冻结值。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class PackageManifestDigestService {

    /** 包清单规范化版本（v1） */
    public static final int CANONICALIZATION_VERSION = 1;

    /** 摘要算法标识 */
    public static final String ALGORITHM = "SHA-256";

    /** 字段号（与 §4.23.2 编码规则同构，禁止改号） */
    private static final int F_PACKAGE_ID = 1;
    private static final int F_PACKAGE_REVISION = 2;
    private static final int F_PLAN_NODE_ID = 3;
    private static final int F_ECU_ID = 4;
    private static final int F_COMPONENT_ID = 5;
    private static final int F_SOFTWARE_TARGET_CODE = 6;
    private static final int F_TARGET_VERSION = 7;
    private static final int F_SIZE_BYTES = 8;
    private static final int F_DIGEST_ALGORITHM = 9;
    private static final int F_DIGEST_HEX = 10;
    private static final int F_DIFFERENTIAL_TYPE = 11;
    private static final int F_ENCRYPTED = 12;

    /**
     * 生成包清单摘要。
     *
     * @param packages           包清单投影（{@code VehicleTaskPackageService.project} 的输出）
     * @param installPlanVersion 安装计划版本
     * @param taskRevision       任务修订号
     * @return SHA-256 小写十六进制摘要
     */
    public String digest(List<DetectionResult.MatchedPackage> packages,
                         String installPlanVersion, Long taskRevision) {
        return sha256(canonicalBytes(packages, installPlanVersion, taskRevision));
    }

    /**
     * 生成发布期密封值（摘要 + 版本 + 可读快照内容）。
     *
     * @param packages           包清单投影
     * @param installPlanVersion 安装计划版本
     * @param taskRevision       任务修订号
     * @return 密封值
     */
    public VehicleTaskSnapshotSeal seal(List<DetectionResult.MatchedPackage> packages,
                                        String installPlanVersion, Long taskRevision) {
        return VehicleTaskSnapshotSeal.builder()
                .snapshotDigest(digest(packages, installPlanVersion, taskRevision))
                .canonicalizationVersion(CANONICALIZATION_VERSION)
                .snapshotContent(snapshotContent(packages, installPlanVersion, taskRevision))
                .build();
    }

    /**
     * 包清单规范化字节（确定性编码）。
     *
     * @param packages           包清单投影
     * @param installPlanVersion 安装计划版本
     * @param taskRevision       任务修订号
     * @return 规范化字节
     */
    public byte[] canonicalBytes(List<DetectionResult.MatchedPackage> packages,
                                 String installPlanVersion, Long taskRevision) {
        StringBuilder sb = new StringBuilder();
        sb.append("canonicalization=").append(CANONICALIZATION_VERSION).append('\n');
        field(sb, 0, "installPlanVersion", installPlanVersion);
        field(sb, 0, "taskRevision", taskRevision == null ? null : String.valueOf(taskRevision));
        List<DetectionResult.MatchedPackage> sorted = sort(packages);
        sb.append("packageCount=").append(sorted.size()).append('\n');
        for (DetectionResult.MatchedPackage p : sorted) {
            field(sb, F_PACKAGE_ID, "packageId", p.getPackageId());
            field(sb, F_PACKAGE_REVISION, "packageRevision", p.getPackageRevision());
            field(sb, F_PLAN_NODE_ID, "planNodeId", p.getPlanNodeId());
            field(sb, F_ECU_ID, "ecuId", p.getEcuId());
            field(sb, F_COMPONENT_ID, "componentId", p.getComponentId());
            field(sb, F_SOFTWARE_TARGET_CODE, "softwareTargetCode", softwareTargetCode(p));
            field(sb, F_TARGET_VERSION, "targetVersion", p.getTargetVersion());
            field(sb, F_SIZE_BYTES, "sizeBytes", p.getSizeBytes() == null ? null : String.valueOf(p.getSizeBytes()));
            field(sb, F_DIGEST_ALGORITHM, "digestAlgorithm", p.getDigestAlgorithm());
            field(sb, F_DIGEST_HEX, "digestHex", p.getDigestHex());
            field(sb, F_DIFFERENTIAL_TYPE, "differentialType", p.getDifferentialType());
            field(sb, F_ENCRYPTED, "encrypted", p.getEncrypted() == null ? null : String.valueOf(p.getEncrypted()));
            sb.append("--\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 结构化快照内容（JSON，便于人工比对；不参与摘要计算）。
     */
    public String snapshotContent(List<DetectionResult.MatchedPackage> packages,
                                  String installPlanVersion, Long taskRevision) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"canonicalizationVersion\":").append(CANONICALIZATION_VERSION)
                .append(",\"installPlanVersion\":").append(json(installPlanVersion))
                .append(",\"taskRevision\":").append(taskRevision == null ? "null" : taskRevision)
                .append(",\"packages\":[");
        List<DetectionResult.MatchedPackage> sorted = sort(packages);
        for (int i = 0; i < sorted.size(); i++) {
            DetectionResult.MatchedPackage p = sorted.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"packageId\":").append(json(p.getPackageId()))
                    .append(",\"packageRevision\":").append(json(p.getPackageRevision()))
                    .append(",\"planNodeId\":").append(json(p.getPlanNodeId()))
                    .append(",\"ecuId\":").append(json(p.getEcuId()))
                    .append(",\"componentId\":").append(json(p.getComponentId()))
                    .append(",\"softwareTargetCode\":").append(json(softwareTargetCode(p)))
                    .append(",\"targetVersion\":").append(json(p.getTargetVersion()))
                    .append(",\"sizeBytes\":").append(p.getSizeBytes() == null ? "null" : p.getSizeBytes())
                    .append(",\"digestAlgorithm\":").append(json(p.getDigestAlgorithm()))
                    .append(",\"digestHex\":").append(json(p.getDigestHex()))
                    .append(",\"differentialType\":").append(json(p.getDifferentialType()))
                    .append(",\"encrypted\":").append(p.getEncrypted() == null ? "null" : p.getEncrypted())
                    .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * 确定性排序：packageId → softwareTargetCode → ecuId → packageRevision。
     */
    private static List<DetectionResult.MatchedPackage> sort(List<DetectionResult.MatchedPackage> packages) {
        List<DetectionResult.MatchedPackage> sorted = packages == null
                ? new ArrayList<>() : new ArrayList<>(packages);
        sorted.sort(Comparator
                .comparing((DetectionResult.MatchedPackage p) -> nz(p.getPackageId()))
                .thenComparing(p -> nz(softwareTargetCode(p)))
                .thenComparing(p -> nz(p.getEcuId()))
                .thenComparing(p -> nz(p.getPackageRevision())));
        return sorted;
    }

    /**
     * 软件目标编码：当前投影未单列该字段时回退为空值，保证两侧编码一致。
     */
    private static String softwareTargetCode(DetectionResult.MatchedPackage p) {
        return "";
    }

    /**
     * 单字段确定性编码：字段号 | 名称 | 类型 | presence | 长度 | 值。
     *
     * <p>空值与空串严格区分（presence 位），避免「缺失」与「空串」产生相同字节。</p>
     */
    private static void field(StringBuilder sb, int no, String name, String value) {
        String normalized = value == null ? null : normalize(value);
        sb.append(no).append('|').append(name).append("|string|")
                .append(normalized == null ? 0 : 1).append('|')
                .append(normalized == null ? 0 : normalized.getBytes(StandardCharsets.UTF_8).length)
                .append('|');
        if (normalized != null) {
            sb.append(normalized);
        }
        sb.append('\n');
    }

    /**
     * 字符串规范化：NFC + trim（UTF-8 编码在取长度时固定）。
     */
    private static String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String json(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
