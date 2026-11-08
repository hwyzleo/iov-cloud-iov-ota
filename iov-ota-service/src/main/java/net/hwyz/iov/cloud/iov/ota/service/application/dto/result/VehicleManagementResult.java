package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 升级车辆管理查询结果（CR-022 §5.1）
 *
 * <p>组合车辆主档只读投影字段与 OTA 运行态 {@code lastInventoryReportedAt}；
 * 该运行态字段不写回 {@code VehicleProjectionPo} / {@code tb_vehicle_projection}（RD-022-1）。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleManagementResult {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 车辆生产业务事实时间
     */
    private Date productionTime;

    /**
     * 生产工厂编码
     */
    private String plantCode;

    /**
     * 品牌编码
     */
    private String brandCode;

    /**
     * 平台编码
     */
    private String platformCode;

    /**
     * 车系编码
     */
    private String carLineCode;

    /**
     * 车型编码
     */
    private String modelCode;

    /**
     * 版本编码
     */
    private String variantCode;

    /**
     * 配置编码，OTA基线和圈车核心锚点
     */
    private String configurationCode;

    /**
     * 最近一次生效的上游事件ID
     */
    private String sourceEventId;

    /**
     * MDM/VMD车辆主档版本
     */
    private Long sourceVersion;

    /**
     * 上游事件发生时间
     */
    private Date sourceEventTime;

    /**
     * 投影最后同步时间
     */
    private Date lastSyncTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后一次有效车辆软件清单上报时间（OTA 服务端时间，只读）
     */
    private Date lastInventoryReportedAt;
}
