package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台监管备案
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegulatoryFilingMpt {

    private Long id;
    private Long activityId;
    private String filingType;
    private String swContentRef;
    private String releaseNoteRef;
    private String filingStatus;
    private String filingNo;

}
