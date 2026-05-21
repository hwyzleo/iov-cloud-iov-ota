package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.api.vo.CompatiblePnExService;
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwarePackageExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionDependencyExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.CompatiblePnAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivitySoftwareBuildVersionVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ConfigWordVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.ActivityPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityCompatiblePnMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityFixedConfigWordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivitySoftwareBuildVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionDependencyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionDependencyPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityCompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityFixedConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.CompatiblePnExServiceAssembler;
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

    private final ActivityMapper activityDao;
    private final CacheService cacheService;
    private final CompatiblePnAppService compatiblePnAppService;
    private final ActivityCompatiblePnMapper activityCompatiblePnDao;
    private final ActivityFixedConfigWordMapper activityFixedConfigWordDao;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;
    private final SoftwareBuildVersionPackageMapper softwareBuildVersionPackageDao;
    private final SoftwareBuildVersionDependencyMapper softwareBuildVersionDependencyDao;
    private final ActivitySoftwareBuildVersionMapper activitySoftwareBuildVersionDao;

    @Override
    public Optional<ActivityDo> getById(Long id) {
        return Optional.ofNullable(cacheService.getActivity(id).orElseGet(() -> {
            ActivityPo activityPo = activityDao.selectPoById(id);
            if (activityPo != null) {
                Map<Integer, List<ActivitySoftwareBuildVersionVo>> groupSoftwareBuildVersionMap = new HashMap<>();
                activitySoftwareBuildVersionDao.selectPoByActivityId(id).forEach(activitySoftwareBuildVersion -> {
                    List<ActivitySoftwareBuildVersionVo> groupList = groupSoftwareBuildVersionMap.computeIfAbsent(activitySoftwareBuildVersion.getVersionGroup(), k -> new ArrayList<>());
                    SoftwareBuildVersionPo softwareBuildVersion = softwareBuildVersionAppService.getSoftwareBuildVersionById(activitySoftwareBuildVersion.getSoftwareBuildVersionId());
                    SoftwareBuildVersionExService softwareBuildVersionExService = SoftwareBuildVersionExServiceAssembler.INSTANCE.fromPo(softwareBuildVersion);
                    groupList.add(ActivitySoftwareBuildVersionVo.builder()
                            .group(activitySoftwareBuildVersion.getVersionGroup())
                            .forceUpgrade(activitySoftwareBuildVersion.getForceUpgrade())
                            .softwareBuildVersion(SoftwareBuildVersionExServiceAssembler.INSTANCE.toVo(softwareBuildVersionExService))
                            .softwarePackageList(SoftwarePackageExServiceAssembler.INSTANCE.toVoList(SoftwarePackageExServiceAssembler.INSTANCE.fromPoList(
                                softwareBuildVersionPackageDao.selectPoBySoftwareBuildVersionId(softwareBuildVersion.getId())
                                    .stream().map(pkgRel -> {
                                        SoftwarePackagePo pkg = new SoftwarePackagePo();
                                        pkg.setId(pkgRel.getSoftwarePackageId());
                                        return pkg;
                                    }).toList())))
                            .softwareBuildVersionDependencyList(SoftwareBuildVersionDependencyExServiceAssembler.INSTANCE.toVoList(
                                SoftwareBuildVersionDependencyExServiceAssembler.INSTANCE.fromPoList(
                                    softwareBuildVersionDependencyDao.selectPoBySoftwareBuildVersionId(softwareBuildVersion.getId()))))
                            .configWordList(new ArrayList<>())
                            .createTime(activitySoftwareBuildVersion.getCreateTime())
                            .build()
                    );
                });
                List<ConfigWordVo> fixedConfigWordList = new ArrayList<>();
                activityFixedConfigWordDao.selectPoByActivityId(id).forEach(activityFixedConfigWord -> {
                });
                Map<String, Set<String>> compatiblePnMap = new HashMap<>();
                activityCompatiblePnDao.selectPoByActivityId(id).forEach(activityCompatiblePn -> {
                    CompatiblePnPo compatiblePn = compatiblePnAppService.getCompatiblePnById(activityCompatiblePn.getCompatiblePnId());
                    if (compatiblePn != null) {
                        CompatiblePnExService compatiblePnExService = CompatiblePnExServiceAssembler.INSTANCE.fromPo(compatiblePn);
                        Set<String> compatiblePnSet = compatiblePnMap.computeIfAbsent(compatiblePnExService.getDeviceCode() + compatiblePnExService.getPn(), k -> new HashSet<>());
                        compatiblePnSet.addAll(List.of(compatiblePnExService.getCompatiblePn().split(",")));
                    }
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
