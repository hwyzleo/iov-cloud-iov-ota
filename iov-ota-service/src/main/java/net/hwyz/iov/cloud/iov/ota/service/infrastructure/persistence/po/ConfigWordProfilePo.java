package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 配置字配置文件表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-11
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_config_word_profile")
public class ConfigWordProfilePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置字代码
     */
    @TableField("config_word_code")
    private String configWordCode;

    /**
     * 配置文件代码
     */
    @TableField("code")
    private String code;

    /**
     * 配置文件名称
     */
    @TableField("name")
    private String name;

    /**
     * 读取类型
     */
    @TableField("read_type")
    private String readType;

    /**
     * 读取ID
     */
    @TableField("read_id")
    private String readId;

    /**
     * 写入类型
     */
    @TableField("write_type")
    private String writeType;

    /**
     * 写入ID
     */
    @TableField("write_id")
    private String writeId;

    /**
     * 会话类型
     */
    @TableField("session_type")
    private String sessionType;

    /**
     * 安全策略
     */
    @TableField("security_level")
    private String securityLevel;

    /**
     * 超时时间
     */
    @TableField("timeout_ms")
    private Integer timeoutMs;

    /**
     * 重试次数
     */
    @TableField("retry_times")
    private Integer retryTimes;

    /**
     * 软件零件号范围
     */
    @TableField("software_pn")
    private String softwarePn;

    /**
     * 软件内部版本范围
     */
    @TableField("software_build_ver")
    private String softwareBuildVer;
}
