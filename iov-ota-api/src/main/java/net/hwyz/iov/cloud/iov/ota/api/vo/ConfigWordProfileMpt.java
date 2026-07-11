package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台配置字配置文件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigWordProfileMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 配置字代码
     */
    private String configWordCode;

    /**
     * 配置文件代码
     */
    private String code;

    /**
     * 配置文件名称
     */
    private String name;

    /**
     * 读取类型
     */
    private String readType;

    /**
     * 读取ID
     */
    private String readId;

    /**
     * 写入类型
     */
    private String writeType;

    /**
     * 写入ID
     */
    private String writeId;

    /**
     * 会话类型
     */
    private String sessionType;

    /**
     * 安全策略
     */
    private String securityLevel;

    /**
     * 超时时间
     */
    private Integer timeoutMs;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 软件零件号范围
     */
    private String softwarePn;

    /**
     * 软件内部版本范围
     */
    private String softwareBuildVer;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
