package net.hwyz.iov.cloud.ota.pota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.ota.pota.api.contract.enums.ConfigWordType;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.ConfigWordDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.FixedConfigWordDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.ConfigWordPo;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.FixedConfigWordPo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 固定配置字应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedConfigWordAppService {

    private final ConfigWordDao configWordDao;
    private final FixedConfigWordDao fixedConfigWordDao;

    /**
     * 查询固定配置字
     *
     * @param deviceCode 设备代码
     * @param type       分类
     * @param softwarePn 软件零件号
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return 固定配置字列表
     */
    public List<FixedConfigWordPo> search(String deviceCode, Integer type, String softwarePn, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceCode", deviceCode);
        map.put("type", type);
        map.put("softwarePn", softwarePn);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return fixedConfigWordDao.selectPoByMap(map);
    }

    /**
     * 根据主键ID获取固定配置字
     *
     * @param id 主键ID
     * @return 固定配置字
     */
    public FixedConfigWordPo getFixedConfigWordById(Long id) {
        return fixedConfigWordDao.selectPoById(id);
    }

    /**
     * 根据主键ID获取配置字
     *
     * @param id 主键ID
     * @return 配置字
     */
    public ConfigWordPo getConfigWordById(Long id) {
        return configWordDao.selectPoById(id);
    }

    /**
     * 根据主键ID获取配置字列表
     *
     * @param fixedConfigWordId 固定配置字ID
     * @return 配置字列表
     */
    public List<ConfigWordPo> listConfigWordByFixedConfigWordId(Long fixedConfigWordId) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", ConfigWordType.FIXED.value);
        map.put("referenceId", fixedConfigWordId);
        return configWordDao.selectPoByMap(map);
    }

    /**
     * 新增固定配置字
     *
     * @param fixedConfigWordPo 固定配置字
     * @return 结果
     */
    public int createFixedConfigWord(FixedConfigWordPo fixedConfigWordPo) {
        return fixedConfigWordDao.insertPo(fixedConfigWordPo);
    }

    /**
     * 新增配置字
     *
     * @param configWordPo 配置字
     * @return 结果
     */
    public int createConfigWord(ConfigWordPo configWordPo) {
        configWordPo.setType(ConfigWordType.FIXED.value);
        return configWordDao.insertPo(configWordPo);
    }

    /**
     * 修改固定配置字
     *
     * @param fixedConfigWordPo 固定配置字
     * @return 结果
     */
    public int modifyFixedConfigWord(FixedConfigWordPo fixedConfigWordPo) {
        return fixedConfigWordDao.updatePo(fixedConfigWordPo);
    }

    /**
     * 修改配置字
     *
     * @param configWordPo 配置字
     * @return 结果
     */
    public int modifyConfigWord(ConfigWordPo configWordPo) {
        return configWordDao.updatePo(configWordPo);
    }

    /**
     * 批量删除固定配置字
     *
     * @param ids 固定配置字ID数组
     * @return 结果
     */
    public int deleteFixedConfigWordByIds(Long[] ids) {
        return fixedConfigWordDao.batchPhysicalDeletePo(ids);
    }

    /**
     * 批量删除固定配置字
     *
     * @param ids 固定配置字ID数组
     * @return 结果
     */
    public int deleteConfigWordByIds(Long[] ids) {
        return configWordDao.batchPhysicalDeletePo(ids);
    }

}
