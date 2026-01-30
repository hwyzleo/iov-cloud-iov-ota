package net.hwyz.iov.cloud.ota.pota.api.feign.oapi;

import net.hwyz.iov.cloud.framework.common.bean.Response;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomBaselineOapi;
import net.hwyz.iov.cloud.ota.pota.api.contract.BomSwoOapi;

/**
 * BOM相关开放平台接口
 *
 * @author hwyz_leo
 */
public interface BomOapiApi {

    /**
     * 同步BOM基线
     *
     * @param bomBaseline BOM基线
     * @return 同步结果
     */
    Response<Void> syncBomBaseline(BomBaselineOapi bomBaseline);

    /**
     * 同步BOM售后变更
     *
     * @param bomSwo BOM售后变更
     * @return 同步结果
     */
    Response<Void> syncBomSwo(BomSwoOapi bomSwo);

}
