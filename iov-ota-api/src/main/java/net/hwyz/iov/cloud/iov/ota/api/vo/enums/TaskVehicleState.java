package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 升级任务车辆升级状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum TaskVehicleState {

    WAITING_DOWNLOAD("等待下载", 0),
    DOWNLOAD_STARTED("开始下载", 1),
    DOWNLOAD_RESUMING("继续下载", 3),
    DOWNLOAD_COMPLETED("结束下载", 5),
    SCHEDULED_UPGRADE("预约升级", 7),
    AUTO_UPGRADE("自动升级", 9),
    INSTALLATION_CHECK("安装检测", 10),
    INSTALLATION_STARTED("开始安装", 11),
    INSTALLATION_COMPLETED("结束安装", 15),
    ROLLBACK_STARTED("开始回滚", 17),
    ROLLBACK_COMPLETED("结束回滚", 19),
    UPGRADE_AUTO_REBOOT("升级立即重启", 21),
    UPGRADE_USER_REBOOT("升级用户重启", 22),
    ROLLBACK_AUTO_REBOOT("回滚立即重启", 23),
    ROLLBACK_USER_REBOOT("回滚用户重启", 24),
    CONFIG_WRITE("写配置字", 25),
    CONFIG_ROLLBACK("回滚配置字", 26),
    UPGRADE_FAILED("升级失败", 90),
    UPGRADE_TIMEOUT("升级超时", 91),
    UPGRADE_SUCCESS("升级成功", 99);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static TaskVehicleState valOf(Integer val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(TaskVehicleState.values())
                .filter(taskVehicleState -> taskVehicleState.value == val)
                .findFirst()
                .orElse(null);
    }

}
