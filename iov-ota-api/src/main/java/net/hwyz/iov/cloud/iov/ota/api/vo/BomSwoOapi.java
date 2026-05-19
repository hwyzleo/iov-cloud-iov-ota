package net.hwyz.iov.cloud.iov.ota.api.vo;

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
public class BomSwoOapi {

    /**
     * SWO编码
     */
    private String code;

    /**
     * SWO状态
     */
    private String state;

    /**
     * SWO类型
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
     * SWO说明
     */
    private String swoDesc;

    /**
     * 软件内部版本列表
     */
    private List<BomSoftwareBuildVersionOapi> softwareBuildVersionList;

}
