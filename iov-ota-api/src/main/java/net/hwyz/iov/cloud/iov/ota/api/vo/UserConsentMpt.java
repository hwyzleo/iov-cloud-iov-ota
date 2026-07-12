package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

import java.util.Date;

/**
 * 管理后台用户授权
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConsentMpt {

    private Long id;
    private Long taskId;
    private String vin;
    private String consentType;
    private Long articleId;
    private Integer consentResult;
    private Date consentTime;

}
