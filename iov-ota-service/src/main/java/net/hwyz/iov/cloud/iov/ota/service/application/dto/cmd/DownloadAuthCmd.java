package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下载授权命令（CR-012 §5.4、US-078）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadAuthCmd {

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 包ID */
    private String packageId;

    /** 包版本号 */
    private String packageRevision;

    /** 对象 ETag */
    private String etag;

    /** 续传偏移量（字节），0 表示从头下载 */
    private Long offset;

    /** 操作类型：DOWNLOAD / RESUME / RESET_OFFSET */
    private String operation;

    /** 车架号 */
    private String vin;
}
