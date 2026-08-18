package net.hwyz.iov.cloud.iov.ota.service.infrastructure.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * VMD 车辆主档游标快照 DTO（CR-015 §4.1）
 * <p>对应 GET /api/service/vehicle/v1/snapshot?cursor=&size=&updatedAfter=；
 * nextCursor 为空表示拉取完毕。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdVehicleProjectionSnapshotDto {

    /** 本页车辆 */
    private List<VmdVehicleProjectionDto> items;

    /** 下一页游标（为空表示结束） */
    private String nextCursor;

    /** 是否还有更多 */
    private Boolean hasMore;
}
