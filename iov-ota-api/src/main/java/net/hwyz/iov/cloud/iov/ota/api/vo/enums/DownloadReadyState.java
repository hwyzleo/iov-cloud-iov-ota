package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 下载准备状态枚举类（CR-012 §2.2、§5.4）
 *
 * <p>下载准备状态归 VehicleTask，与安装事件通道分离。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum DownloadReadyState {

    NOT_STARTED("未开始"),
    IN_PROGRESS("进行中"),
    VERIFIED("已校验"),
    FAILED("已失败");

    public final String label;
    public final String value = name();

    public static DownloadReadyState valOf(String val) {
        if (val == null) {
            return null;
        }
        for (DownloadReadyState state : values()) {
            if (state.value.equals(val)) {
                return state;
            }
        }
        return null;
    }
}
