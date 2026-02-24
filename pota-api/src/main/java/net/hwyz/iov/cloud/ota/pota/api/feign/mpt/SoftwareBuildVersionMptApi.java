package net.hwyz.iov.cloud.ota.pota.api.feign.mpt;

import jakarta.servlet.http.HttpServletResponse;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionMpt;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwarePackageMpt;

/**
 * 软件内部版本信息相关管理后台接口
 *
 * @author hwyz_leo
 */
public interface SoftwareBuildVersionMptApi {

    /**
     * 分页查询软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 软件内部版本信息列表
     */
    TableDataInfo list(SoftwareBuildVersionMpt softwareBuildVersion);

    /**
     * 查询软件内部版本下软件包
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwarePackage        软件包
     * @return 软件包列表
     */
    AjaxResult listSoftwarePackage(Long softwareBuildVersionId, SoftwarePackageMpt softwarePackage);

    /**
     * 查询软件内部版本下依赖的软件内部版本
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwareBuildVersion   软件内部版本信息
     * @return 依赖的软件内部版本列表
     */
    AjaxResult listDependency(Long softwareBuildVersionId, SoftwareBuildVersionMpt softwareBuildVersion);

    /**
     * 导出软件内部版本信息
     *
     * @param response             响应
     * @param softwareBuildVersion 软件内部版本信息
     */
    void export(HttpServletResponse response, SoftwareBuildVersionMpt softwareBuildVersion);

    /**
     * 根据软件内部版本信息ID获取软件内部版本信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     * @return 软件内部版本信息
     */
    AjaxResult getInfo(Long softwareBuildVersionId);

    /**
     * 新增软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 结果
     */
    AjaxResult add(SoftwareBuildVersionMpt softwareBuildVersion);

    /**
     * 新增关联的软件包
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwarePackageIds     软件包ID数组
     * @return 结果
     */
    AjaxResult addSoftwarePackage(Long softwareBuildVersionId, Long[] softwarePackageIds);

    /**
     * 新增依赖的软件依赖版本
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @param adaptiveLevel           适配级别
     * @return 结果
     */
    AjaxResult addDependency(Long softwareBuildVersionId, Long[] softwareBuildVersionIds, Integer adaptiveLevel);

    /**
     * 修改保存软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 结果
     */
    AjaxResult edit(SoftwareBuildVersionMpt softwareBuildVersion);

    /**
     * 修改保存依赖的软件依赖版本
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @param adaptiveLevel           适配级别
     * @return 结果
     */
    AjaxResult editDependency(Long softwareBuildVersionId, Long[] softwareBuildVersionIds, Integer adaptiveLevel);

    /**
     * 删除软件内部版本信息
     *
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    AjaxResult remove(Long[] softwareBuildVersionIds);

    /**
     * 删除关联的软件包
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwarePackageIds     软件包ID数组
     * @return 结果
     */
    AjaxResult removeSoftwarePackage(Long softwareBuildVersionId, Long[] softwarePackageIds);

    /**
     * 删除依赖的软件内部版本
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    AjaxResult removeDependency(Long softwareBuildVersionId, Long[] softwareBuildVersionIds);

}
