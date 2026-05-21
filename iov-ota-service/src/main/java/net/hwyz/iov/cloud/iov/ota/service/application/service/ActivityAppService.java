package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.api.vo.ActivitySoftwareBuildVersionMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityCompatiblePnMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityFixedConfigWordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivitySoftwareBuildVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityCompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityFixedConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivitySoftwareBuildVersionPo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 升级活动应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAppService {

    private final ActivityMapper activityDao;
    private final ActivityCompatiblePnMapper activityCompatiblePnDao;
    private final ActivityFixedConfigWordMapper activityFixedConfigWordDao;
    private final ActivitySoftwareBuildVersionMapper activitySoftwareBuildVersionDao;

    /**
     * 查询升级活动
     *
     * @param name      升级活动名称
     * @param state     升级活动状态
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 升级活动列表
     */
    public List<ActivityPo> search(String name, Integer state, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("state", state);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return activityDao.selectPoByMap(map);
    }

    /**
     * 获取升级活动下的软件内部版本列表
     *
     * @param activityId 升级活动ID
     * @return 软件内部版本列表
     */
    public List<ActivitySoftwareBuildVersionPo> listSoftwareBuildVersion(Long activityId) {
        return activitySoftwareBuildVersionDao.selectPoByActivityId(activityId);
    }

    /**
     * 获取升级活动下的兼容零件号列表
     *
     * @param activityId 升级活动ID
     * @return 兼容零件号列表
     */
    public List<ActivityCompatiblePnPo> listCompatiblePn(Long activityId) {
        return activityCompatiblePnDao.selectPoByExample(ActivityCompatiblePnPo.builder().activityId(activityId).build());
    }

    /**
     * 获取升级活动下的兼容零件号列表
     *
     * @param activityId 升级活动ID
     * @return 兼容零件号列表
     */
    public List<ActivityFixedConfigWordPo> listFixedConfigWord(Long activityId) {
        return activityFixedConfigWordDao.selectPoByExample(ActivityFixedConfigWordPo.builder().activityId(activityId).build());
    }

    /**
     * 根据主键ID获取升级活动
     *
     * @param id 主键ID
     * @return 升级活动
     */
    public ActivityPo getActivityById(Long id) {
        return activityDao.selectPoById(id);
    }

    /**
     * 新增升级活动
     *
     * @param activity                         升级活动
     * @param activitySoftwareBuildVersionList 活动软件内部版本列表
     * @return 结果
     */
    public int createActivity(ActivityPo activity, List<ActivitySoftwareBuildVersionPo> activitySoftwareBuildVersionList) {
        activity.setState(ActivityState.PENDING.value);
        int result = activityDao.insertPo(activity);
        if (activitySoftwareBuildVersionList != null && !activitySoftwareBuildVersionList.isEmpty()) {
            activitySoftwareBuildVersionList.forEach(po -> {
                po.setActivityId(activity.getId());
                po.setVersionGroup(0);
            });
            activitySoftwareBuildVersionDao.batchInsertPo(activitySoftwareBuildVersionList);
        }
        return result;
    }

    /**
     * 新增升级活动软件内部版本信息
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    public int createSoftwareBuildVersion(Long activityId, Long[] softwareBuildVersionIds) {
        Set<Long> softwareBuildVersionIdSet = listSoftwareBuildVersion(activityId).stream()
                .map(ActivitySoftwareBuildVersionPo::getSoftwareBuildVersionId)
                .collect(Collectors.toSet());
        List<ActivitySoftwareBuildVersionPo> list = new ArrayList<>();
        for (Long softwareBuildVersionId : softwareBuildVersionIds) {
            if (!softwareBuildVersionIdSet.contains(softwareBuildVersionId)) {
                list.add(ActivitySoftwareBuildVersionPo.builder()
                        .activityId(activityId)
                        .softwareBuildVersionId(softwareBuildVersionId)
                        .sort(0)
                        .versionGroup(0)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return activitySoftwareBuildVersionDao.batchInsertPo(list);
        }
        return 0;
    }

    /**
     * 新增升级活动兼容零件号
     *
     * @param activityId      升级活动ID
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    public int createCompatiblePn(Long activityId, Long[] compatiblePnIds) {
        Set<Long> compatiblePnIdSet = listCompatiblePn(activityId).stream()
                .map(ActivityCompatiblePnPo::getCompatiblePnId)
                .collect(Collectors.toSet());
        List<ActivityCompatiblePnPo> list = new ArrayList<>();
        for (Long compatiblePnId : compatiblePnIds) {
            if (!compatiblePnIdSet.contains(compatiblePnId)) {
                list.add(ActivityCompatiblePnPo.builder()
                        .activityId(activityId)
                        .compatiblePnId(compatiblePnId)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return activityCompatiblePnDao.batchInsertPo(list);
        }
        return 0;
    }

    /**
     * 新增升级活动固定配置字
     *
     * @param activityId         升级活动ID
     * @param fixedConfigWordIds 固定配置字ID数组
     * @return 结果
     */
    public int createFixedConfigWord(Long activityId, Long[] fixedConfigWordIds) {
        Set<Long> fixedConfigWordIdSet = listFixedConfigWord(activityId).stream()
                .map(ActivityFixedConfigWordPo::getFixedConfigWordId)
                .collect(Collectors.toSet());
        List<ActivityFixedConfigWordPo> list = new ArrayList<>();
        for (Long fixedConfigWordId : fixedConfigWordIds) {
            if (!fixedConfigWordIdSet.contains(fixedConfigWordId)) {
                list.add(ActivityFixedConfigWordPo.builder()
                        .activityId(activityId)
                        .fixedConfigWordId(fixedConfigWordId)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return activityFixedConfigWordDao.batchInsertPo(list);
        }
        return 0;
    }

    /**
     * 修改升级活动
     *
     * @param activity 升级活动
     * @return 结果
     */
    public int modifyActivity(ActivityPo activity) {
        return activityDao.updatePo(activity);
    }

    /**
     * 修改升级活动软件内部版本信息
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @param sorts                   排序数组
     * @param groups                  组数数组
     * @return 结果
     */
    public int modifyActivitySoftwareBuildVersion(Long activityId, Long[] softwareBuildVersionIds, Integer[] sorts, Integer[] groups) {
        listSoftwareBuildVersion(activityId).forEach(po -> {
            for (int i = 0; i < softwareBuildVersionIds.length; i++) {
                if (po.getSoftwareBuildVersionId().longValue() == softwareBuildVersionIds[i]) {
                    po.setSort(sorts[i]);
                    po.setVersionGroup(groups[i]);
                    activitySoftwareBuildVersionDao.updatePo(po);
                }
            }
        });
        return 1;
    }

    /**
     * 批量删除升级活动
     *
     * @param ids 升级活动ID数组
     * @return 结果
     */
    public int deleteActivityByIds(Long[] ids) {
        return activityDao.batchPhysicalDeletePo(ids);
    }

    /**
     * 删除升级活动软件内部版本信息
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    public int deleteSoftwareBuildVersion(Long activityId, Long[] softwareBuildVersionIds) {
        return activitySoftwareBuildVersionDao.batchPhysicalDeletePoByActivityIdAndSoftwareBuildVersionIds(activityId, softwareBuildVersionIds);
    }

    /**
     * 删除升级活动兼容零件号信息
     *
     * @param activityId      升级活动ID
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    public int deleteCompatiblePn(Long activityId, Long[] compatiblePnIds) {
        return activityCompatiblePnDao.batchPhysicalDeletePoByActivityIdAndCompatiblePnIds(activityId, compatiblePnIds);
    }

    /**
     * 删除升级活动固定配置字信息
     *
     * @param activityId         升级活动ID
     * @param fixedConfigWordIds 固定配置字ID数组
     * @return 结果
     */
    public int deleteFixedConfigWord(Long activityId, Long[] fixedConfigWordIds) {
        return activityFixedConfigWordDao.batchPhysicalDeletePoByActivityIdAndFixedConfigWordIds(activityId, fixedConfigWordIds);
    }

    /**
     * 重组升级活动软件内部版本信息
     *
     * @param activityId 升级活动ID
     * @param list       组数数组
     * @return 结果
     */
    public int regroupActivitySoftwareBuildVersion(Long activityId, List<ActivitySoftwareBuildVersionMpt> list) {
        AtomicInteger result = new AtomicInteger();
        listSoftwareBuildVersion(activityId).forEach(po -> {
            list.forEach(mpt -> {
                if (po.getId().longValue() == mpt.getId()) {
                    if (po.getVersionGroup().intValue() != mpt.getVersionGroup()) {
                        po.setVersionGroup(mpt.getVersionGroup());
                        po.setSort(0);
                        activitySoftwareBuildVersionDao.updatePo(po);
                        result.getAndIncrement();
                    }
                }
            });
        });
        return result.get();
    }

    /**
     * 重排序升级活动软件内部版本信息
     *
     * @param activityId 升级活动ID
     * @param list       组数数组
     * @return 结果
     */
    public int resortActivitySoftwareBuildVersion(Long activityId, List<ActivitySoftwareBuildVersionMpt> list) {
        AtomicInteger result = new AtomicInteger();
        listSoftwareBuildVersion(activityId).forEach(po -> {
            list.forEach(mpt -> {
                if (po.getId().longValue() == mpt.getId()) {
                    if (po.getSort().intValue() != mpt.getSort()) {
                        po.setSort(mpt.getSort());
                        activitySoftwareBuildVersionDao.updatePo(po);
                        result.getAndIncrement();
                    }
                }
            });
        });
        return result.get();
    }

    /**
     * 统计基线软件内部版本数量
     *
     * @param activityId 升级活动ID
     * @return 基线软件零件版本数量
     */
    public int countActivitySoftwareBuildVersion(Long activityId) {
        return activitySoftwareBuildVersionDao.countByActivityId(activityId);
    }

}
