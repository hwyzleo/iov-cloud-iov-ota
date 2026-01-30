package net.hwyz.iov.cloud.ota.pota.api.feign.mpt;

import jakarta.servlet.http.HttpServletResponse;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.ConfigWordMpt;
import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordMpt;

/**
 * 固定配置字相关管理后台接口
 *
 * @author hwyz_leo
 */
public interface FixedConfigWordMptApi {

    /**
     * 分页查询固定配置字
     *
     * @param fixedConfigWord 固定配置字
     * @return 固定配置字列表
     */
    TableDataInfo list(FixedConfigWordMpt fixedConfigWord);

    /**
     * 列出配置字列表
     *
     * @param fixedConfigWordId 固定配置字ID
     * @return 配置字列表
     */
    AjaxResult listConfigWord(Long fixedConfigWordId);

    /**
     * 导出固定配置字
     *
     * @param response        响应
     * @param fixedConfigWord 固定配置字
     */
    void export(HttpServletResponse response, FixedConfigWordMpt fixedConfigWord);

    /**
     * 根据固定配置字ID获取固定配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @return 固定配置字
     */
    AjaxResult getInfo(Long fixedConfigWordId);

    /**
     * 根据配置字ID获取配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @param configWordId      配置字ID
     * @return 配置字
     */
    AjaxResult getConfigWordInfo(Long fixedConfigWordId, Long configWordId);

    /**
     * 新增固定配置字
     *
     * @param fixedConfigWord 固定配置字
     * @return 结果
     */
    AjaxResult add(FixedConfigWordMpt fixedConfigWord);

    /**
     * 新增配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @param configWord        配置字
     * @return 结果
     */
    AjaxResult addConfigWord(Long fixedConfigWordId, ConfigWordMpt configWord);

    /**
     * 修改保存固定配置字
     *
     * @param fixedConfigWord 固定配置字
     * @return 结果
     */
    AjaxResult edit(FixedConfigWordMpt fixedConfigWord);

    /**
     * 修改保存配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @param configWord        配置字
     * @return 结果
     */
    AjaxResult editConfigWord(Long fixedConfigWordId, ConfigWordMpt configWord);

    /**
     * 删除固定配置字
     *
     * @param fixedConfigWordIds 固定配置字ID数组
     * @return 结果
     */
    AjaxResult remove(Long[] fixedConfigWordIds);

    /**
     * 删除配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @param configWordIds     配置字ID数组
     * @return 结果
     */
    AjaxResult removeConfigWord(Long fixedConfigWordId, Long[] configWordIds);

}
