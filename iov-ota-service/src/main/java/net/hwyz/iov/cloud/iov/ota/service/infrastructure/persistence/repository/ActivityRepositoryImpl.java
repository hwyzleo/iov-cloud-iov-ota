package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwarePackageExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionDependencyExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityUpgradeTargetVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ConfigWordVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwareBuildVersionVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.ActivityPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityFixedConfigWordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityUpgradeTargetMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionDependencyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionDependencyPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityFixedConfigWordPo;
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
    private final ActivityFixedConfigWordMapper activityFixedConfigWordDao;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;
    private final SoftwareBuildVersionPackageMapper softwareBuildVersionPackageDao;
    private final SoftwareBuildVersionDependencyMapper softwareBuildVersionDependencyDao;
    private final ActivityUpgradeTargetMapper activityUpgradeTargetDao;

    @Override
    public Optional<ActivityDo> getById(Long id) {
        return Optional.ofNullable(cacheService.getActivity(id).orElseGet(() -> {
            ActivityPo activityPo = activityDao.selectPoById(id);
            if (activityPo != null) {
                Map<Integer, List<ActivityUpgradeTargetVo>> groupUpgradeTargetMap = new HashMap<>();
                activityUpgradeTargetDao.selectPoByActivityId(id).forEach(target -> {
                    List<ActivityUpgradeTargetVo> groupList = groupUpgradeTargetMap.computeIfAbsent(target.getGroupNo() != null ? target.getGroupNo() : 0, k -> new ArrayList<>());
                    SoftwareBuildVersionVo sbvVo = null;
                    if (target.getSoftwareBuildVersionId() != null) {
                        SoftwareBuildVersionPo softwareBuildVersion = softwareBuildVersionAppService.getSoftwareBuildVersionById(target.getSoftwareBuildVersionId());
                        SoftwareBuildVersionExService softwareBuildVersionExService = SoftwareBuildVersionExServiceAssembler.INSTANCE.fromPo(softwareBuildVersion);
                        sbvVo = SoftwareBuildVersionExServiceAssembler.INSTANCE.toVo(softwareBuildVersionExService);
                    }
                    groupList.add(ActivityUpgradeTargetVo.builder()
                            .id(target.getId())
                            .activityId(target.getActivityId())
                            .sourceType(target.getSourceType())
                            .baselineCode(target.getBaselineCode())
                            .vehicleNodeCode(target.getVehicleNodeCode())
                            .partCode(target.getPartCode())
                            .softwareBuildVersionId(target.getSoftwareBuildVersionId())
                            .critical(target.getCritical())
                            .ota(target.getOta())
                            .installSeq(target.getInstallSeq())
                            .parallelGroup(target.getParallelGroup())
                            .groupNo(target.getGroupNo())
                            .forceUpgrade(target.getForceUpgrade())
                            .softwareBuildVersion(sbvVo)
                            .softwarePackageList(sbvVo != null ? SoftwarePackageExServiceAssembler.INSTANCE.toVoList(SoftwarePackageExServiceAssembler.INSTANCE.fromPoList(
                                softwareBuildVersionPackageDao.selectPoBySoftwareBuildVersionId(target.getSoftwareBuildVersionId())
                                    .stream().map(pkgRel -> {
                                        SoftwarePackagePo pkg = new SoftwarePackagePo();
                                        pkg.setId(pkgRel.getSoftwarePackageId());
                                        return pkg;
                                    }).toList())) : new ArrayList<>())
                            .softwareBuildVersionDependencyList(sbvVo != null ? SoftwareBuildVersionDependencyExServiceAssembler.INSTANCE.toVoList(
                                SoftwareBuildVersionDependencyExServiceAssembler.INSTANCE.fromPoList(
                                    softwareBuildVersionDependencyDao.selectPoBySoftwareBuildVersionId(target.getSoftwareBuildVersionId()))) : new ArrayList<>())
                            .configWordList(new ArrayList<>())
                            .createTime(target.getCreateTime())
                            .build()
                    );
                });
                List<ConfigWordVo> fixedConfigWordList = new ArrayList<>();
                activityFixedConfigWordDao.selectPoByActivityId(id).forEach(activityFixedConfigWord -> {
                });
                ActivityDo activityDoTmp = ActivityPoAssembler.INSTANCE.toDo(activityPo);
                activityDoTmp.load(groupUpgradeTargetMap, fixedConfigWordList);
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
