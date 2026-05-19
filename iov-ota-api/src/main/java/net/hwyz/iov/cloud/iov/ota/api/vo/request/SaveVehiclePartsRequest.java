package net.hwyz.iov.cloud.iov.ota.api.vo.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehiclePartExService;

import java.util.List;

/**
 * 保存车辆零件信息请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveVehiclePartsRequest {

    /**
     * 车辆零件列表
     */
    @NotEmpty(message = "车辆零件列表不能为空")
    private List<VehiclePartExService> vehiclePartList;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 备注
     */
    private String remark;

}
