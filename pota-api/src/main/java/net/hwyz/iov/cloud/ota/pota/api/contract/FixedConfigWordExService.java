package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 对外服务固定配置字
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedConfigWordExService {

    /**
     * 主键
     */
    private Long id;

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 软件零件号
     */
    private String softwarePn;

    /**
     * 分类
     */
    private Integer type;

    /**
     * 描述
     */
    private String description;

    /**
     * 配置字列表
     */
    private List<ConfigWordExService> configWordList;

    /**
     * 创建时间
     */
    private Date createTime;

}
