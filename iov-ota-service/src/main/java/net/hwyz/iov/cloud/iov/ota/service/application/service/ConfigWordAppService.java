package net.hwyz.iov.cloud.iov.ota.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ConfigWordFieldMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ConfigWordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ConfigWordProfileMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordFieldPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordProfilePo;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    private final ConfigWordMapper configWordMapper;
    private final ConfigWordFieldMapper configWordFieldMapper;
    private final ConfigWordProfileMapper configWordProfileMapper;

    /**
     * 查询配置字
     *
     * @param deviceCode 设备代码
     * @param code       配置字代码
     * @param name       配置字名称
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return 配置字列表
     */
    public List<ConfigWordPo> search(String deviceCode, String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceCode", deviceCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return configWordMapper.selectPoByMap(map);
    }

    /**
     * 查询配置字配置文件
     *
     * @param configWordCode 配置字代码
     * @param code           配置字配置文件代码
     * @param name           配置字配置文件名称
     * @param beginTime      开始时间
     * @param endTime        结束时间
     * @return 配置字配置文件列表
     */
    public List<ConfigWordProfilePo> searchProfile(String configWordCode, String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("configWordCode", configWordCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return configWordProfileMapper.selectPoByMap(map);
    }

    /**
     * 查询配置字字段
     *
     * @param configWordCode        配置字代码
     * @param configWordProfileCode 配置字配置文件代码
     * @param code                  配置字代码
     * @param name                  配置字名称
     * @param beginTime             开始时间
     * @param endTime               结束时间
     * @return 配置字列表
     */
    public List<ConfigWordFieldPo> searchField(String configWordCode, String configWordProfileCode, String code, String name,
                                               Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("configWordCode", configWordCode);
        map.put("configWordProfileCode", configWordProfileCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return configWordFieldMapper.selectPoByMap(map);
    }

    /**
     * 检查配置字代码是否唯一
     *
     * @param configWordId 配置字ID
     * @param code         配置字代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long configWordId, String code) {
        if (ObjUtil.isNull(configWordId)) {
            configWordId = -1L;
        }
        ConfigWordPo configWordPo = getConfigWordByCode(code);
        return !ObjUtil.isNotNull(configWordPo) || configWordPo.getId().longValue() == configWordId.longValue();
    }

    /**
     * 检查配置字配置文件代码是否唯一
     *
     * @param configWordProfileId 配置字配置文件ID
     * @param configWordCode      配置字代码
     * @param code                配置字配置文件代码
     * @return 结果
     */
    public Boolean checkProfileCodeUnique(Long configWordProfileId, String configWordCode, String code) {
        if (ObjUtil.isNull(configWordProfileId)) {
            configWordProfileId = -1L;
        }
        ConfigWordProfilePo configWordProfilePo = getConfigWordProfileByCode(configWordCode, code);
        return !ObjUtil.isNotNull(configWordProfilePo) || configWordProfilePo.getId().longValue() == configWordProfileId.longValue();
    }

    /**
     * 检查配置字字段代码是否唯一
     *
     * @param configWordFieldId     配置字字段ID
     * @param configWordCode        配置字代码
     * @param configWordProfileCode 配置字配置文件代码
     * @param code                  配置字字段代码
     * @return 结果
     */
    public Boolean checkFieldCodeUnique(Long configWordFieldId, String configWordCode, String configWordProfileCode, String code) {
        if (ObjUtil.isNull(configWordFieldId)) {
            configWordFieldId = -1L;
        }
        ConfigWordFieldPo configWordFieldPo = getConfigWordFieldByCode(configWordCode, configWordProfileCode, code);
        return !ObjUtil.isNotNull(configWordFieldPo) || configWordFieldPo.getId().longValue() == configWordFieldId.longValue();
    }

    /**
     * 根据主键ID获取配置字
     *
     * @param id 主键ID
     * @return 配置字
     */
    public ConfigWordPo getConfigWordById(Long id) {
        return configWordMapper.selectPoById(id);
    }

    /**
     * 根据主键ID获取配置字配置文件
     *
     * @param configWordCode 配置字代码
     * @param id             主键ID
     * @return 配置字配置文件
     */
    public ConfigWordProfilePo getConfigWordProfileById(String configWordCode, Long id) {
        return configWordProfileMapper.selectPoById(id);
    }

    /**
     * 根据主键ID获取配置字字段
     *
     * @param configWordCode        配置字代码
     * @param configWordProfileCode 配置字配置文件代码
     * @param id                    主键ID
     * @return 配置字字段
     */
    public ConfigWordFieldPo getConfigWordFieldById(String configWordCode, String configWordProfileCode, Long id) {
        return configWordFieldMapper.selectPoById(id);
    }

    /**
     * 根据配置字代码获取配置字
     *
     * @param code 配置字代码
     * @return 配置字
     */
    public ConfigWordPo getConfigWordByCode(String code) {
        return configWordMapper.selectPoByCode(code);
    }

    /**
     * 根据配置字配置文件代码获取配置字配置文件
     *
     * @param configWordCode 配置字代码
     * @param code           配置字配置文件代码
     * @return 配置字配置文件
     */
    public ConfigWordProfilePo getConfigWordProfileByCode(String configWordCode, String code) {
        return configWordProfileMapper.selectPoByConfigWordCodeAndCode(configWordCode, code);
    }

    /**
     * 根据配置字代码获取配置字
     *
     * @param configWordCode        配置字代码
     * @param configWordProfileCode 配置字配置文件代码
     * @param code                  配置字字段代码
     * @return 配置字
     */
    public ConfigWordFieldPo getConfigWordFieldByCode(String configWordCode, String configWordProfileCode, String code) {
        return configWordFieldMapper.selectPoByCode(configWordCode, configWordProfileCode, code);
    }

    /**
     * 新增配置字
     *
     * @param configWordPo 配置字
     * @return 结果
     */
    public int createConfigWord(ConfigWordPo configWordPo) {
        return configWordMapper.insertPo(configWordPo);
    }

    /**
     * 新增配置字配置文件
     *
     * @param configWordCode    配置字代码
     * @param configWordProfile 配置字配置文件
     * @return 结果
     */
    public int createConfigWordProfile(String configWordCode, ConfigWordProfilePo configWordProfile) {
        return configWordProfileMapper.insertPo(configWordProfile);
    }

    /**
     * 新增配置字字段
     *
     * @param configWordCode    配置字代码
     * @param configWordFieldPo 配置字字段
     * @return 结果
     */
    public int createConfigWordField(String configWordCode, ConfigWordFieldPo configWordFieldPo) {
        return configWordFieldMapper.insertPo(configWordFieldPo);
    }

    /**
     * 修改配置字
     *
     * @param configWordPo 配置字
     * @return 结果
     */
    public int modifyConfigWord(ConfigWordPo configWordPo) {
        return configWordMapper.updatePo(configWordPo);
    }

    /**
     * 修改配置字配置文件
     *
     * @param configWordCode    配置字代码
     * @param configWordProfile 配置字配置文件
     * @return 结果
     */
    public int modifyConfigWordProfile(String configWordCode, ConfigWordProfilePo configWordProfile) {
        return configWordProfileMapper.updatePo(configWordProfile);
    }

    /**
     * 修改配置字字段
     *
     * @param configWordCode    配置字代码
     * @param configWordFieldPo 配置字字段
     * @return 结果
     */
    public int modifyConfigWordField(String configWordCode, ConfigWordFieldPo configWordFieldPo) {
        return configWordFieldMapper.updatePo(configWordFieldPo);
    }

    /**
     * 批量删除配置字
     *
     * @param ids 配置字ID数组
     * @return 结果
     */
    public int deleteConfigWordByIds(Long[] ids) {
        return configWordMapper.batchPhysicalDeletePo(ids);
    }

    /**
     * 批量删除配置字配置文件
     *
     * @param configWordCode 配置字代码
     * @param ids            配置字配置文件ID数组
     * @return 结果
     */
    public int deleteConfigWordProfileByIds(String configWordCode, Long[] ids) {
        return configWordProfileMapper.batchPhysicalDeletePo(ids);
    }

    /**
     * 批量删除配置字字段
     *
     * @param configWordCode        配置字代码
     * @param configWordProfileCode 配置字配置文件代码
     * @param ids                   配置字字段ID数组
     * @return 结果
     */
    public int deleteConfigWordFieldByIds(String configWordCode, String configWordProfileCode, Long[] ids) {
        return configWordFieldMapper.batchPhysicalDeletePo(ids);
    }

}
