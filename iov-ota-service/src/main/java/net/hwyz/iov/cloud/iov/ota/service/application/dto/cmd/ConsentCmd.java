package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户授权命令（CR-012 §5.3、US-077）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentCmd {

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 授权动作：GRANT / DENY / REVOKE */
    private String action;

    /** 条款文章ID */
    private Long termsId;

    /** 条款摘要 */
    private String termsHash;

    /** 授权范围摘要 */
    private String consentScopeDigest;

    /** 授权回执ID（幂等键） */
    private String consentReceiptId;

    /** 车架号 */
    private String vin;
}
