package net.hwyz.iov.cloud.ota.pota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomSoftwarePackageOapi;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.BomSoftwarePackageOapiAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao.SoftwarePackageDao;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwarePackagePo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 软件包信息应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwarePackageAppService {

    private final SoftwarePackageDao softwarePackageDao;

    /**
     * 查询软件包信息
     *
     * @param deviceCode             设备代码
     * @param softwarePn             软件零件号
     * @param packageCode            软件包代码
     * @param packageName            软件包名称
     * @param softwareBuildVersionId 软件内部版本ID
     * @param beginTime              开始时间
     * @param endTime                结束时间
     * @return 软件包列表
     */
    public List<SoftwarePackagePo> search(String deviceCode, String softwarePn, String packageCode, String packageName,
                                          Long softwareBuildVersionId, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceCode", deviceCode);
        map.put("softwarePn", softwarePn);
        map.put("packageCode", packageCode);
        map.put("packageName", ParamHelper.fuzzyQueryParam(packageName));
        map.put("softwareBuildVersionId", softwareBuildVersionId);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return softwarePackageDao.selectPoByMap(map);
    }

    /**
     * 列出软件内部版本下的软件包信息
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @return 软件包列表
     */
    public List<SoftwarePackagePo> listBySoftwareBuildVersionId(Long softwareBuildVersionId) {
        Map<String, Object> map = new HashMap<>();
        map.put("softwareBuildVersionId", softwareBuildVersionId);
        return softwarePackageDao.selectPoByMap(map);
    }

    /**
     * 根据主键ID获取软件包信息
     *
     * @param id 主键ID
     * @return 软件包信息
     */
    public SoftwarePackagePo getSoftwarePackageById(Long id) {
        return softwarePackageDao.selectPoById(id);
    }

    /**
     * 新增软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    public int createSoftwarePackage(SoftwarePackagePo softwarePackage) {
        return softwarePackageDao.insertPo(softwarePackage);
    }

    /**
     * 新增BOM软件包信息
     *
     * @param bomSoftwarePackage BOM软件包信息
     * @return 结果
     */
    public SoftwarePackagePo createSoftwarePackage(BomSoftwarePackageOapi bomSoftwarePackage) {
        SoftwarePackagePo softwarePackagePo = BomSoftwarePackageOapiAssembler.INSTANCE.toPo(bomSoftwarePackage);
        softwarePackageDao.insertPo(softwarePackagePo);
        return softwarePackagePo;
    }

    /**
     * 修改软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    public int modifySoftwarePackage(SoftwarePackagePo softwarePackage) {
        return softwarePackageDao.updatePo(softwarePackage);
    }

    /**
     * 批量删除软件包信息
     *
     * @param ids 软件包信息ID数组
     * @return 结果
     */
    public int deleteSoftwarePackageByIds(Long[] ids) {
        return softwarePackageDao.batchPhysicalDeletePo(ids);
    }

}
