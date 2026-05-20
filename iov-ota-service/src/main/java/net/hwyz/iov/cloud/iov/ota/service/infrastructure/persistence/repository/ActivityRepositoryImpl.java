package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.activity.model.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.activity.model.ActivitySoftwareBuildVersionVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.activity.model.ConfigWordVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.activity.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.ConfigWordExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.SoftwareBuildVersionDependencyExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.SoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.SoftwarePackageExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.assembler.ActivityPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.ActivityCompatiblePnDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.ActivityDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.ActivityFixedConfigWordDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.ActivitySoftwareBuildVersionDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityPo;
import net.hwyz.iov.cloud.ota.pota.api.contract.CompatiblePnExService;
import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordExService;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExCompatiblePnService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExFixedConfigWordService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExSoftwareBuildVersionService;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 升级活动仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityRepositoryImpl extends AbstractRepository<Long, ActivityDo> implements ActivityRepository {

    private final ActivityDao activityDao;
    private final CacheService cacheService;
    private final ExCompatiblePnService exCompatiblePnService;
    private final ActivityCompatiblePnDao activityCompatiblePnDao;
    private final ExFixedConfigWordService exFixedConfigWordService;
    private final ActivityFixedConfigWordDao activityFixedConfigWordDao;
    private final ExSoftwareBuildVersionService exSoftwareBuildVersionService;
    private final ActivitySoftwareBuildVersionDao activitySoftwareBuildVersionDao;

    @Override
    public Optional<ActivityDo> getById(Long id) {
        return Optional.ofNullable(cacheService.getActivity(id).orElseGet(() -> {
            ActivityPo activityPo = activityDao.selectPoById(id);
            if (activityPo != null) {
                Map<Integer, List<ActivitySoftwareBuildVersionVo>> groupSoftwareBuildVersionMap = new HashMap<>();
                activitySoftwareBuildVersionDao.selectPoByActivityId(id).forEach(activitySoftwareBuildVersion -> {
                    List<ActivitySoftwareBuildVersionVo> groupList = groupSoftwareBuildVersionMap.computeIfAbsent(activitySoftwareBuildVersion.getVersionGroup(), k -> new ArrayList<>());
                    SoftwareBuildVersionExService softwareBuildVersion = exSoftwareBuildVersionService.getInfo(activitySoftwareBuildVersion.getSoftwareBuildVersionId());
                    groupList.add(ActivitySoftwareBuildVersionVo.builder()
                            .group(activitySoftwareBuildVersion.getVersionGroup())
                            .forceUpgrade(activitySoftwareBuildVersion.getForceUpgrade())
                            .softwareBuildVersion(SoftwareBuildVersionExServiceAssembler.INSTANCE.toVo(softwareBuildVersion))
                            .softwarePackageList(SoftwarePackageExServiceAssembler.INSTANCE.toVoList(softwareBuildVersion.getSoftwarePackageList()))
                            .softwareBuildVersionDependencyList(SoftwareBuildVersionDependencyExServiceAssembler.INSTANCE.toVoList(softwareBuildVersion.getDependencyList()))
                            .configWordList(ConfigWordExServiceAssembler.INSTANCE.toVoList(softwareBuildVersion.getConfigWordList()))
                            .createTime(activitySoftwareBuildVersion.getCreateTime())
                            .build()
                    );
                });
                List<ConfigWordVo> fixedConfigWordList = new ArrayList<>();
                activityFixedConfigWordDao.selectPoByActivityId(id).forEach(activityFixedConfigWord -> {
                    FixedConfigWordExService fixedConfigWord = exFixedConfigWordService.getInfo(activityFixedConfigWord.getFixedConfigWordId());
                    fixedConfigWord.getConfigWordList().forEach(configWord -> {
                        ConfigWordVo vo = ConfigWordExServiceAssembler.INSTANCE.toVo(configWord);
                        vo.setDeviceCode(fixedConfigWord.getDeviceCode());
                        vo.setSoftwarePn(fixedConfigWord.getSoftwarePn());
                        fixedConfigWordList.add(vo);
                    });
                });
                Map<String, Set<String>> compatiblePnMap = new HashMap<>();
                activityCompatiblePnDao.selectPoByActivityId(id).forEach(activityCompatiblePn -> {
                    CompatiblePnExService compatiblePn = exCompatiblePnService.getInfo(activityCompatiblePn.getCompatiblePnId());
                    Set<String> compatiblePnSet = compatiblePnMap.computeIfAbsent(compatiblePn.getDeviceCode() + compatiblePn.getPn(), k -> new HashSet<>());
                    compatiblePnSet.addAll(List.of(compatiblePn.getCompatiblePn().split(",")));
                });
                ActivityDo activityDoTmp = ActivityPoAssembler.INSTANCE.toDo(activityPo);
                activityDoTmp.load(groupSoftwareBuildVersionMap, fixedConfigWordList, compatiblePnMap);
                cacheService.setActivity(activityDoTmp);
                return activityDoTmp;
            }
            return null;
        }));
    }

    @Override
    public boolean save(ActivityDo activityDo) {
        switch (activityDo.getState()) {
            case CHANGED -> {
                ActivityPo activityPo = ActivityPoAssembler.INSTANCE.fromDo(activityDo);
                activityDao.updatePo(activityPo);
                cacheService.setActivity(activityDo);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

}
