package net.hwyz.iov.cloud.iov.ota.api.feign.ccp;

import net.hwyz.iov.cloud.framework.common.bean.Response;
import net.hwyz.iov.cloud.iov.ota.api.contract.CloudFotaInfoCcp;
import net.hwyz.iov.cloud.iov.ota.api.contract.TaskVehicleProcessCcp;
import net.hwyz.iov.cloud.iov.ota.api.contract.TaskVehicleStateCcp;
import net.hwyz.iov.cloud.iov.ota.api.contract.VehicleFotaInfoCcp;

/**
 * 在线固件升级中央计算平台接口
 *
 * @author hwyz_leo
 */
public interface FotaCcpService {

    /**
     * 检查车辆升级信息
     *
     * @param vin             车架号
     * @param vehicleFotaInfo 车辆升级信息
     * @return 云端升级信息
     */
    Response<CloudFotaInfoCcp> check(String vin, VehicleFotaInfoCcp vehicleFotaInfo);

    /**
     * 上报车辆升级任务过程
     *
     * @param vin                车架号
     * @param taskVehicleProcess 车辆升级任务过程
     * @return 上报结果
     */
    Response<Void> reportTaskProcess(String vin, TaskVehicleProcessCcp taskVehicleProcess);

    /**
     * 上报车辆升级任务状态
     *
     * @param vin              车架号
     * @param taskVehicleState 车辆升级任务状态
     * @return 上报结果
     */
    Response<Void> reportTaskState(String vin, TaskVehicleStateCcp taskVehicleState);

}
