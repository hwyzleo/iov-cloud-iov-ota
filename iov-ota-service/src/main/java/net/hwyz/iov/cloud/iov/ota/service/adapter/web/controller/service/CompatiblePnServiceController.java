package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.iov.ota.api.vo.CompatiblePnExService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.CompatiblePnAppService;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.CompatiblePnExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.CompatiblePnNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 兼容零件号相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/compatiblePn/v1")
public class CompatiblePnServiceController extends BaseController {

    private final CompatiblePnAppService compatiblePnAppService;

    /**
     * 分页查询兼容零件号列表
     *
     * @param compatiblePn 兼容零件号
     * @return 兼容零件号列表
     */
    @GetMapping(value = "/list")
    public PageResult<CompatiblePnExService> list(CompatiblePnExService compatiblePn) {
        log.info("查询兼容零件号列表");
        startPage();
        List<CompatiblePnPo> compatiblePnPoList = compatiblePnAppService.search(compatiblePn.getDeviceCode(),
                compatiblePn.getType(), null, null);
        return getPageResult(PageUtil.convert(compatiblePnPoList, CompatiblePnExServiceAssembler.INSTANCE::fromPo));
    }

    /**
     * 根据兼容零件号ID获取兼容零件号信息
     *
     * @param compatiblePnId 兼容零件号ID
     * @return 兼容零件号信息
     */
    @GetMapping(value = "/{compatiblePnId}")
    public CompatiblePnExService getInfo(@PathVariable Long compatiblePnId) {
        log.info("根据兼容零件号ID[{}]获取兼容零件号信息", compatiblePnId);
        CompatiblePnPo compatiblePnPo = compatiblePnAppService.getCompatiblePnById(compatiblePnId);
        if (ObjUtil.isNull(compatiblePnPo)) {
            throw new CompatiblePnNotExistException(compatiblePnId);
        }
        return CompatiblePnExServiceAssembler.INSTANCE.fromPo(compatiblePnPo);
    }

}
