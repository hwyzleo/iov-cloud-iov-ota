package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.framework.common.domain.DoState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityFixedConfigWordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityUpgradeTargetMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionDependencyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityRepositoryImpl 状态持久化与缓存一致性")
class ActivityRepositoryImplTest {

    @Mock private ActivityMapper activityDao;
    @Mock private CacheService cacheService;
    @Mock private ActivityFixedConfigWordMapper activityFixedConfigWordDao;
    @Mock private SoftwareBuildVersionAppService softwareBuildVersionAppService;
    @Mock private SoftwareBuildVersionPackageMapper softwareBuildVersionPackageDao;
    @Mock private SoftwareBuildVersionDependencyMapper softwareBuildVersionDependencyDao;
    @Mock private ActivityUpgradeTargetMapper activityUpgradeTargetDao;

    @InjectMocks
    private ActivityRepositoryImpl repository;

    private ActivityPo pendingActivityPo(Long id) {
        ActivityPo po = new ActivityPo();
        po.setId(id);
        po.setState(ActivityState.PENDING.value);
        return po;
    }

    @Nested
    @DisplayName("save 脏标记复位（回归：发布后状态回退为待审核）")
    class SaveResetsDirtyFlag {

        @Test
        @DisplayName("save 成功后脏标记复位，被阻断的 release 不再整行回写过期状态")
        void save_resetsDirtyFlag_blockedReleaseWontRewrite() {
            Long activityId = 1L;

            // DB 初始为待提交，getById 走缓存未命中 -> 全量加载
            when(activityDao.selectPoById(activityId)).thenReturn(pendingActivityPo(activityId));
            when(activityUpgradeTargetDao.selectPoByActivityId(activityId)).thenReturn(Collections.emptyList());

            ActivityDo activityDo = repository.getById(activityId).orElseThrow();
            assertEquals(ActivityState.PENDING, activityDo.getActivityState());

            // 1. 提交：PENDING -> SUBMITTED 并落库
            assertEquals(1, activityDo.submit(new ActivityPo()));
            assertTrue(repository.save(activityDo));
            // 修复点：save 后脏标记必须复位，否则后续无实际变更的操作也会误触发整行回写
            assertEquals(DoState.UNCHANGED, activityDo.getState());
            verify(activityDao).updatePo(argThat(po -> po.getState() == ActivityState.SUBMITTED.value));

            // 2. 模拟审批直写 DB 已置为已审核，但当前内存对象仍停留在待审核（审批走 DAO 直写，缓存未同步）
            //    —— 修复前：残留 CHANGED + 过期 SUBMITTED 会让 release 的 save 把 DB 回写成待审核
            //    修复后：release 被阻断且不产生任何 DB 写
            assertEquals(0, activityDo.release());
            assertFalse(repository.save(activityDo));
            verify(activityDao, times(1)).updatePo(any());
        }

        @Test
        @DisplayName("缓存返回过期待审核对象时，release 被阻断且无 DB 回写")
        void blockedReleaseWithStaleCache_doesNotRewriteDb() {
            Long activityId = 1L;

            // 构造缓存中过期的领域对象：状态停留在待审核
            ActivityDo staleDo = ActivityDo.builder()
                    .id(activityId)
                    .activityState(ActivityState.SUBMITTED)
                    .build();
            staleDo.stateLoad(); // 已持久化过的对象应为 UNCHANGED（修复前 save 未复位导致的残留 CHANGED 即 bug 根源）
            when(cacheService.getActivity(activityId)).thenReturn(Optional.of(staleDo));

            ActivityDo loaded = repository.getById(activityId).orElseThrow();
            assertEquals(ActivityState.SUBMITTED, loaded.getActivityState());

            // 状态非已审核 -> 发布被阻断
            assertEquals(0, loaded.release());
            // 脏标记 UNCHANGED -> save 不写库，DB 保持已审核不被回写
            assertFalse(repository.save(loaded));
            verify(activityDao, never()).updatePo(any());
        }
    }
}
