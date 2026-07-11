package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.Date;

/**
 * tb_swin_definition 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_swin_definition")
public class SwinDefinitionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * SWIN代码
     */
    @TableField("swin_code")
    private String swinCode;

    /**
     * 编码方案代码
     */
    @TableField("scheme_code")
    private String schemeCode;

    /**
     * 引用类型
     */
    @TableField("type_ref_type")
    private String typeRefType;

    /**
     * 引用代码
     */
    @TableField("type_ref_code")
    private String typeRefCode;

    /**
     * 名称
     */
    @TableField("name")
    private String name;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

    /**
     * 最后同步时间
     */
    @TableField("sync_time")
    private Date syncTime;

}
