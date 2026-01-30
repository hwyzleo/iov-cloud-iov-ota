package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 开放平台BOM基线信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomBaselineOapi {

    /**
     * 基线编码
     */
    private String code;

    /**
     * 基线名称
     */
    private String name;

    /**
     * 基线类型
     */
    private String type;

    /**
     * 车型编码
     */
    private String vehModel;

    /**
     * 发布日期
     */
    private Date releaseDate;

    /**
     * 基线说明
     */
    private String baselineDesc;

    /**
     * 软件内部版本列表
     */
    private List<BomSoftwareBuildVersionOapi> softwareBuildVersionList;

}
