package net.hwyz.iov.cloud.ota.pota.api.feign.mpt;

import jakarta.servlet.http.HttpServletResponse;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwarePackageMpt;

/**
 * 软件包信息相关管理后台接口
 *
 * @author hwyz_leo
 */
public interface SoftwarePackageMptApi {

    /**
     * 分页查询软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 软件包信息列表
     */
    TableDataInfo list(SoftwarePackageMpt softwarePackage);

    /**
     * 导出软件包信息
     *
     * @param response        响应
     * @param softwarePackage 软件包信息
     */
    void export(HttpServletResponse response, SoftwarePackageMpt softwarePackage);

    /**
     * 根据软件包信息ID获取软件包信息
     *
     * @param softwarePackageId 软件包信息ID
     * @return 软件包信息
     */
    AjaxResult getInfo(Long softwarePackageId);

    /**
     * 新增软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    AjaxResult add(SoftwarePackageMpt softwarePackage);

    /**
     * 修改保存软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    AjaxResult edit(SoftwarePackageMpt softwarePackage);

    /**
     * 删除软件包信息
     *
     * @param softwarePackageIds 软件零件包ID数组
     * @return 结果
     */
    AjaxResult remove(Long[] softwarePackageIds);

}
