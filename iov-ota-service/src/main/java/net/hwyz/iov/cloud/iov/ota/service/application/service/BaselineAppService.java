package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.BaselineMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselinePo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 基线应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaselineAppService {

    private final BaselineMapper baselineMapper;

    /**
     * 查询基线
     *
     * @param baselineCode  基线代码
     * @param name          基线名称
     * @param anchorCode    锚定代码
     * @param baselineStatus 基线状态
     * @param beginTime     开始时间
     * @param endTime       结束时间
     * @return 基线列表
     */
    public List<BaselinePo> search(String baselineCode, String name, String anchorCode,
                                   String baselineStatus, Date beginTime, Date endTime) {
        LambdaQueryWrapper<BaselinePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ParamHelper.fuzzyQueryParam(baselineCode) != null, BaselinePo::getBaselineCode, baselineCode);
        wrapper.like(ParamHelper.fuzzyQueryParam(name) != null, BaselinePo::getName, name);
        wrapper.eq(anchorCode != null && !anchorCode.isBlank(), BaselinePo::getAnchorCode, anchorCode);
        wrapper.eq(baselineStatus != null && !baselineStatus.isBlank(), BaselinePo::getBaselineStatus, baselineStatus);
        wrapper.ge(beginTime != null, BaselinePo::getSyncTime, beginTime);
        wrapper.le(endTime != null, BaselinePo::getSyncTime, endTime);
        wrapper.orderByDesc(BaselinePo::getSyncTime);
        return baselineMapper.selectList(wrapper);
    }

    /**
     * 根据主键ID获取基线
     *
     * @param id 主键ID
     * @return 基线
     */
    public BaselinePo getBaselineById(Long id) {
        return baselineMapper.selectById(id);
    }

}
