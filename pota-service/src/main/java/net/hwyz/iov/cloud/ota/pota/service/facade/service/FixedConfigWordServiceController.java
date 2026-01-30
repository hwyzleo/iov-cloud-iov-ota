package net.hwyz.iov.cloud.ota.pota.service.facade.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordExService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExFixedConfigWordService;
import net.hwyz.iov.cloud.ota.pota.service.application.service.FixedConfigWordAppService;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.ConfigWordExServiceAssembler;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.FixedConfigWordExServiceAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.exception.FixedConfigWordNotExistException;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.ConfigWordPo;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.FixedConfigWordPo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 固定配置字相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/service/fixedConfigWord")
public class FixedConfigWordServiceController extends BaseController implements ExFixedConfigWordService {

    private final FixedConfigWordAppService fixedConfigWordAppService;

    /**
     * 分页查询固定配置字列表
     *
     * @param fixedConfigWord 固定配置字
     * @return 固定配置字列表
     */
    @GetMapping(value = "/list")
    public TableDataInfo list(FixedConfigWordExService fixedConfigWord) {
        logger.info("查询固定配置字列表");
        startPage();
        List<FixedConfigWordPo> fixedConfigWordPoList = fixedConfigWordAppService.search(fixedConfigWord.getDeviceCode(),
                fixedConfigWord.getType(), fixedConfigWord.getSoftwarePn(), null, null);
        List<FixedConfigWordExService> fixedConfigWordExServiceList = FixedConfigWordExServiceAssembler.INSTANCE.fromPoList(fixedConfigWordPoList);
        return getDataTable(fixedConfigWordPoList, fixedConfigWordExServiceList);
    }

    /**
     * 根据固定配置字ID获取固定配置字信息
     *
     * @param fixedConfigWordId 固定配置字ID
     * @return 固定配置字信息
     */
    @GetMapping(value = "/{fixedConfigWordId}")
    public FixedConfigWordExService getInfo(@PathVariable Long fixedConfigWordId) {
        logger.info("根据固定配置字ID[{}]获取固定配置字信息", fixedConfigWordId);
        FixedConfigWordPo fixedConfigWordPo = fixedConfigWordAppService.getFixedConfigWordById(fixedConfigWordId);
        if (ObjUtil.isNull(fixedConfigWordPo)) {
            throw new FixedConfigWordNotExistException(fixedConfigWordId);
        }
        FixedConfigWordExService fixedConfigWordExService = FixedConfigWordExServiceAssembler.INSTANCE.fromPo(fixedConfigWordPo);
        List<ConfigWordPo> configWordPoList = fixedConfigWordAppService.listConfigWordByFixedConfigWordId(fixedConfigWordId);
        fixedConfigWordExService.setConfigWordList(ConfigWordExServiceAssembler.INSTANCE.fromPoList(configWordPoList));
        return fixedConfigWordExService;
    }

}
