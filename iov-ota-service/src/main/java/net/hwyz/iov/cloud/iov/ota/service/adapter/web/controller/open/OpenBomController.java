package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.open;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.iov.ota.api.vo.BomBaselineOapi;
import net.hwyz.iov.cloud.iov.ota.api.vo.BomSwoOapi;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DataSource;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DataType;
import net.hwyz.iov.cloud.iov.ota.service.application.service.DataSyncRecordAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.DataSyncRecordPo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BOM相关开放平台接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/open/bom/v1")
public class OpenBomController extends BaseController {

    private final DataSyncRecordAppService dataSyncRecordAppService;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;

    /**
     * 同步BOM基线
     *
     * @param bomBaseline BOM基线
     * @return 同步结果
     */
    @PostMapping("/action/syncBaseline")
    public ApiResponse<Void> syncBomBaseline(@Validated @RequestBody BomBaselineOapi bomBaseline) {
        log.info("开放平台同步BOM基线[{}]", bomBaseline.getCode());
        DataSyncRecordPo dataSyncRecord = dataSyncRecordAppService.createDataSyncRecord(DataSource.BOM, DataType.BASELINE,
                bomBaseline.getCode(), JSONUtil.toJsonStr(bomBaseline));
        try {
            dataSyncRecordAppService.markRecordSuccess(dataSyncRecord);
        } catch (Exception e) {
            dataSyncRecordAppService.markRecordFail(dataSyncRecord, e.getMessage());
        }
        return new ApiResponse<>();
    }

    /**
     * 同步BOM售后变更
     *
     * @param bomSwo BOM售后变更
     * @return 同步结果
     */
    @PostMapping("/action/syncSwo")
    public ApiResponse<Void> syncBomSwo(@Validated @RequestBody BomSwoOapi bomSwo) {
        log.info("开放平台同步BOM售后变更[{}]", bomSwo.getCode());
        DataSyncRecordPo dataSyncRecord = dataSyncRecordAppService.createDataSyncRecord(DataSource.BOM, DataType.SWO,
                bomSwo.getCode(), JSONUtil.toJsonStr(bomSwo));
        try {
            softwareBuildVersionAppService.createSoftwareBuildVersions(bomSwo.getSoftwareBuildVersionList());
            dataSyncRecordAppService.markRecordSuccess(dataSyncRecord);
        } catch (Exception e) {
            dataSyncRecordAppService.markRecordFail(dataSyncRecord, e.getMessage());
        }
        return new ApiResponse<>();
    }
}
