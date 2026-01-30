package net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 数据同步记录表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-30
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_data_sync_record")
public class DataSyncRecordPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 同步来源
     */
    @TableField("source")
    private Integer source;

    /**
     * 同步类型
     */
    @TableField("type")
    private Integer type;

    /**
     * 代码
     */
    @TableField("code")
    private String code;

    /**
     * 同步数据
     */
    @TableField("data")
    private String data;

    /**
     * 同步状态：0-开始同步，1-同步成功，-1-同步失败
     */
    @TableField("state")
    private Integer state;
}
