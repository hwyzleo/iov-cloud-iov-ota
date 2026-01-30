package net.hwyz.iov.cloud.ota.pota.service.facade.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.CompatiblePnExService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExCompatiblePnService;
import net.hwyz.iov.cloud.ota.pota.service.application.service.CompatiblePnAppService;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.CompatiblePnExServiceAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.exception.CompatiblePnNotExistException;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.CompatiblePnPo;
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
@RequestMapping(value = "/service/compatiblePn")
public class CompatiblePnServiceController extends BaseController implements ExCompatiblePnService {

    private final CompatiblePnAppService compatiblePnAppService;

    /**
     * 分页查询兼容零件号列表
     *
     * @param compatiblePn 兼容零件号
     * @return 兼容零件号列表
     */
    @GetMapping(value = "/list")
    public TableDataInfo list(CompatiblePnExService compatiblePn) {
        logger.info("查询兼容零件号列表");
        startPage();
        List<CompatiblePnPo> compatiblePnPoList = compatiblePnAppService.search(compatiblePn.getDeviceCode(),
                compatiblePn.getType(), null, null);
        List<CompatiblePnExService> compatiblePnExServiceList = CompatiblePnExServiceAssembler.INSTANCE.fromPoList(compatiblePnPoList);
        return getDataTable(compatiblePnPoList, compatiblePnExServiceList);
    }

    /**
     * 根据兼容零件号ID获取兼容零件号信息
     *
     * @param compatiblePnId 兼容零件号ID
     * @return 兼容零件号信息
     */
    @GetMapping(value = "/{compatiblePnId}")
    public CompatiblePnExService getInfo(@PathVariable Long compatiblePnId) {
        logger.info("根据兼容零件号ID[{}]获取兼容零件号信息", compatiblePnId);
        CompatiblePnPo compatiblePnPo = compatiblePnAppService.getCompatiblePnById(compatiblePnId);
        if (ObjUtil.isNull(compatiblePnPo)) {
            throw new CompatiblePnNotExistException(compatiblePnId);
        }
        return CompatiblePnExServiceAssembler.INSTANCE.fromPo(compatiblePnPo);
    }

}
