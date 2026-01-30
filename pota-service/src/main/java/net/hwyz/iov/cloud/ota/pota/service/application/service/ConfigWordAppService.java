package net.hwyz.iov.cloud.ota.pota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.ota.pota.api.contract.enums.ConfigWordType;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.ConfigWordDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.ConfigWordPo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置字应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigWordAppService {

    private final ConfigWordDao configWordDao;

    /**
     * 根据类型和关联ID获取配置字
     *
     * @param type        类型
     * @param referenceId 关联ID
     * @return 配置字
     */
    public List<ConfigWordPo> listConfigWordByTypeAndReferenceId(ConfigWordType type, Long referenceId) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", type.value);
        params.put("referenceId", referenceId);
        return configWordDao.selectPoByMap(params);
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
     * 新增配置字
     *
     * @param configWordPo 配置字
     * @return 结果
     */
    public int createConfigWord(ConfigWordPo configWordPo) {
        return configWordDao.insertPo(configWordPo);
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
    public int deleteConfigWordByIds(Long[] ids) {
        return configWordDao.batchPhysicalDeletePo(ids);
    }

}
