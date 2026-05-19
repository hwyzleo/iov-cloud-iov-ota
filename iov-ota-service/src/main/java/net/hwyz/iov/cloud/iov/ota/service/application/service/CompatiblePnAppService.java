package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.CompatiblePnMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 兼容零件号应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompatiblePnAppService {

    private final CompatiblePnMapper compatiblePnMapper;

    /**
     * 查询兼容零件号
     *
     * @param deviceCode 设备代码
     * @param type       分类
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return 兼容零件号列表
     */
    public List<CompatiblePnPo> search(String deviceCode, Integer type, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceCode", deviceCode);
        map.put("type", type);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return compatiblePnMapper.selectPoByMap(map);
    }

    /**
     * 根据主键ID获取兼容零件号
     *
     * @param id 主键ID
     * @return 兼容零件号
     */
    public CompatiblePnPo getCompatiblePnById(Long id) {
        return compatiblePnMapper.selectPoById(id);
    }

    /**
     * 新增兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 结果
     */
    public int createCompatiblePn(CompatiblePnPo compatiblePn) {
        return compatiblePnMapper.insertPo(compatiblePn);
    }

    /**
     * 修改兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 结果
     */
    public int modifyCompatiblePn(CompatiblePnPo compatiblePn) {
        return compatiblePnMapper.updatePo(compatiblePn);
    }

    /**
     * 批量删除兼容零件号
     *
     * @param ids 兼容零件号ID数组
     * @return 结果
     */
    public int deleteCompatiblePnByIds(Long[] ids) {
        return compatiblePnMapper.batchPhysicalDeletePo(ids);
    }

}
