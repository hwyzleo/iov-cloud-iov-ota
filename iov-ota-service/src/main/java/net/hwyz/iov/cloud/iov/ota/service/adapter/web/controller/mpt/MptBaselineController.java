package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.iov.ota.api.vo.BaselineMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.BaselineMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.BaselineAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselinePo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 基线相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/baseline/v1")
public class MptBaselineController extends BaseController {

    private final BaselineAppService baselineAppService;

    /**
     * 分页查询基线
     *
     * @param baseline 基线
     * @return 基线列表
     */
    @RequiresPermissions("ota:baseline:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<BaselineMpt>> list(BaselineMpt baseline) {
        log.info("管理后台用户[{}]分页查询基线", SecurityUtils.getUsername());
        startPage();
        List<BaselinePo> baselinePoList = baselineAppService.search(baseline.getBaselineCode(),
                baseline.getName(), baseline.getAnchorCode(), baseline.getBaselineStatus(),
                getBeginTime(baseline), getEndTime(baseline));
        return ApiResponse.ok(getPageResult(PageUtil.convert(baselinePoList, BaselineMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 根据基线ID获取基线
     *
     * @param baselineId 基线ID
     * @return 基线
     */
    @RequiresPermissions("ota:baseline:query")
    @GetMapping(value = "/{baselineId}")
    public ApiResponse<BaselineMpt> getInfo(@PathVariable Long baselineId) {
        log.info("管理后台用户[{}]根据基线ID[{}]获取基线", SecurityUtils.getUsername(), baselineId);
        BaselinePo baselinePo = baselineAppService.getBaselineById(baselineId);
        return ApiResponse.ok(BaselineMptAssembler.INSTANCE.fromPo(baselinePo));
    }

}
