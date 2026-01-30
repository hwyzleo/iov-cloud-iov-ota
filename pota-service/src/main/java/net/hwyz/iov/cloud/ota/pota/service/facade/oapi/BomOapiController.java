package net.hwyz.iov.cloud.ota.pota.service.facade.oapi;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.Response;
import net.hwyz.iov.cloud.framework.common.web.controller.BaseController;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomBaselineOapi;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomSwoOapi;
import net.hwyz.iov.cloud.ota.pota.api.contract.enums.DataSource;
import net.hwyz.iov.cloud.ota.pota.api.contract.enums.DataType;
import net.hwyz.iov.cloud.ota.pota.api.feign.oapi.BomOapiApi;
import net.hwyz.iov.cloud.ota.pota.service.application.service.DataSyncRecordAppService;
import net.hwyz.iov.cloud.ota.pota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.DataSyncRecordPo;
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
@RequestMapping(value = "/oapi/bom")
public class BomOapiController extends BaseController implements BomOapiApi {

    private final DataSyncRecordAppService dataSyncRecordAppService;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;

    /**
     * 同步BOM基线
     *
     * @param bomBaseline BOM基线
     * @return 同步结果
     */
    @Override
    @PostMapping("/action/syncBaseline")
    public Response<Void> syncBomBaseline(@Validated @RequestBody BomBaselineOapi bomBaseline) {
        logger.info("开放平台同步BOM基线[{}]", bomBaseline.getCode());
        DataSyncRecordPo dataSyncRecord = dataSyncRecordAppService.createDataSyncRecord(DataSource.BOM, DataType.BASELINE,
                bomBaseline.getCode(), JSONUtil.toJsonStr(bomBaseline));
        try {
            dataSyncRecordAppService.markRecordSuccess(dataSyncRecord);
        } catch (Exception e) {
            dataSyncRecordAppService.markRecordFail(dataSyncRecord, e.getMessage());
        }
        return new Response<>();
    }

    /**
     * 同步BOM售后变更
     *
     * @param bomSwo BOM售后变更
     * @return 同步结果
     */
    @Override
    @PostMapping("/action/syncSwo")
    public Response<Void> syncBomSwo(@Validated @RequestBody BomSwoOapi bomSwo) {
        logger.info("开放平台同步BOM售后变更[{}]", bomSwo.getCode());
        DataSyncRecordPo dataSyncRecord = dataSyncRecordAppService.createDataSyncRecord(DataSource.BOM, DataType.SWO,
                bomSwo.getCode(), JSONUtil.toJsonStr(bomSwo));
        try {
            softwareBuildVersionAppService.createSoftwareBuildVersions(bomSwo.getSoftwareBuildVersionList());
            dataSyncRecordAppService.markRecordSuccess(dataSyncRecord);
        } catch (Exception e) {
            dataSyncRecordAppService.markRecordFail(dataSyncRecord, e.getMessage());
        }
        return new Response<>();
    }
}
