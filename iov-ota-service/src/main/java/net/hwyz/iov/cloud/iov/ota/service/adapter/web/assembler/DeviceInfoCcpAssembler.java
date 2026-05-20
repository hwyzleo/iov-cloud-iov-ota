package net.hwyz.iov.cloud.iov.ota.service.facade.assembler;

import net.hwyz.iov.cloud.iov.ota.api.contract.DeviceInfoCcp;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.DeviceInfoVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 中央计算平台设备信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface DeviceInfoCcpAssembler {

    DeviceInfoCcpAssembler INSTANCE = Mappers.getMapper(DeviceInfoCcpAssembler.class);

    /**
     * 值对象转数据传输对象
     *
     * @param deviceInfoVo 值对象
     * @return 数据传输对象
     */
    @Mappings({})
    DeviceInfoCcp fromVo(DeviceInfoVo deviceInfoVo);

    /**
     * 数据传输对象转值对象
     *
     * @param deviceInfoCcp 数据传输对象
     * @return 值对象
     */
    @Mappings({})
    DeviceInfoVo toVo(DeviceInfoCcp deviceInfoCcp);

    /**
     * 值对象列表转数据传输对象列表
     *
     * @param deviceInfoVoList 值对象列表
     * @return 数据传输对象列表
     */
    List<DeviceInfoCcp> fromVoList(List<DeviceInfoVo> deviceInfoVoList);

    /**
     * 数据传输对象列表转值对象列表
     *
     * @param deviceInfoCcpList 数据传输对象列表
     * @return 值对象列表
     */
    List<DeviceInfoVo> toVoList(List<DeviceInfoCcp> deviceInfoCcpList);

}
