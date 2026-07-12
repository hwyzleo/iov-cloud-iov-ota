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
 * 软件内部版本测试报告 数据对象
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_software_build_version_test_report")
public class SoftwareBuildVersionTestReportPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("sbv_id")
    private Long sbvId;

    @TableField("report_url")
    private String reportUrl;

    @TableField("report_type")
    private String reportType;

    @TableField("test_state")
    private String testState;

    @TableField("verdict")
    private String verdict;

    @TableField("tested_at")
    private Date testedAt;

    @TableField("tested_by")
    private String testedBy;
}
