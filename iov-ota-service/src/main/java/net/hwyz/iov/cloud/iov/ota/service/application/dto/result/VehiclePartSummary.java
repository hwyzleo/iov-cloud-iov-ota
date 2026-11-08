package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 车辆零件投影摘要（IOV-OTA-DSN-CR-023 §4.3）
 *
 * <p>仅承载 VMD 零件投影事实，配置字原样只读返回，不与 OTA 清单字段合并或覆盖。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePartSummary {

    private Long id;

    private String pn;

    private String deviceCode;

    private String deviceItem;

    private String sn;

    /** 原始配置字，只读；null 由前端展示「--」 */
    private String configWord;

    private String supplierCode;

    private String batchNum;

    private String hardwareVer;

    private String softwareVer;

    private String hardwarePn;

    private String softwarePn;

    private String extra;

    private Date modifyTime;
}
