package net.hwyz.iov.cloud.ota.pota.api.feign.mpt;

import jakarta.servlet.http.HttpServletResponse;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.CompatiblePnMpt;

/**
 * 兼容零件号相关管理后台接口
 *
 * @author hwyz_leo
 */
public interface CompatiblePnMptApi {

    /**
     * 分页查询兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 兼容零件号列表
     */
    TableDataInfo list(CompatiblePnMpt compatiblePn);

    /**
     * 导出固定配置字
     *
     * @param response     响应
     * @param compatiblePn 兼容零件号
     */
    void export(HttpServletResponse response, CompatiblePnMpt compatiblePn);

    /**
     * 根据兼容零件号ID获取兼容零件号
     *
     * @param compatiblePnId 兼容零件号ID
     * @return 兼容零件号
     */
    AjaxResult getInfo(Long compatiblePnId);

    /**
     * 新增兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 结果
     */
    AjaxResult add(CompatiblePnMpt compatiblePn);

    /**
     * 修改保存兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 结果
     */
    AjaxResult edit(CompatiblePnMpt compatiblePn);

    /**
     * 删除兼容零件号
     *
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    AjaxResult remove(Long[] compatiblePnIds);

}
