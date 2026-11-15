package net.hwyz.iov.cloud.iov.ota.service.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 车辆任务快照密封值（CR-024 §4.28.3）。
 *
 * <p>发布期一次性冻结：包清单摘要（规范化算法）+ canonicalization 版本 + 结构化快照内容。
 * 由应用层算法服务生成、由仓储与 VehicleTask 同事务持久化，检测与许可期只读取不再重算。</p>
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class VehicleTaskSnapshotSeal {

    /** 包清单摘要（SHA-256 小写十六进制） */
    private final String snapshotDigest;

    /** 包清单规范化版本（摘要空间隔离，禁止跨版本比较） */
    private final int canonicalizationVersion;

    /** 结构化快照内容（JSON，规范化前的可读形式） */
    private final String snapshotContent;
}
