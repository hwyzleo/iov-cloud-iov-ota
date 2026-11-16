package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 软件包信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-30
 */
@Mapper
public interface SoftwarePackageMapper extends BaseDao<SoftwarePackagePo, Long> {

    /**
     * 按软件包代码读取权威包大小（IOV-OTA-DSN-CR-026 §7.5 fail-closed）。
     *
     * @param packageCode 包代码
     * @return 包字节数；不存在或未登记为 null
     */
    @org.apache.ibatis.annotations.Select(
            "SELECT package_size FROM tb_software_package WHERE package_code = #{packageCode} AND row_valid = 1 LIMIT 1")
    Long selectPackageSizeByCode(@org.apache.ibatis.annotations.Param("packageCode") String packageCode);

}
