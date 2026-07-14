package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityUpgradeTargetPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityAppService 双重发布门禁")
class ActivityAppServiceTest {

    @Mock private ActivityMapper activityDao;
    @Mock private ActivityFixedConfigWordMapper activityFixedConfigWordDao;
    @Mock private ActivityUpgradeTargetMapper activityUpgradeTargetDao;
    @Mock private ActivityGroupPolicyMapper activityGroupPolicyDao;
    @Mock private SoftwareBuildVersionAppService softwareBuildVersionAppService;

    @InjectMocks
    private ActivityAppService appService;

    @Nested
    @DisplayName("createUpgradeTarget - 门禁校验")
    class CreateUpgradeTargetWithGate {

        @Test
        @DisplayName("SBV 通过门禁 -> 绑定成功")
        void gatePassed_bindsSbv() {
            Long activityId = 1L;
            Long sbvId = 10L;

            when(activityUpgradeTargetDao.selectPoByActivityId(activityId))
                    .thenReturn(Collections.emptyList());
            when(softwareBuildVersionAppService.checkReleaseGate(sbvId)).thenReturn(true);
            when(activityUpgradeTargetDao.batchInsertPo(any())).thenReturn(1);

            int result = appService.createUpgradeTarget(activityId, new Long[]{sbvId});

            assertEquals(1, result);
            verify(softwareBuildVersionAppService).checkReleaseGate(sbvId);
        }

        @Test
        @DisplayName("SBV 未通过门禁 -> 阻断绑定")
        void gateFailed_blocksBinding() {
            Long activityId = 1L;
            Long sbvId = 10L;

            when(activityUpgradeTargetDao.selectPoByActivityId(activityId))
                    .thenReturn(Collections.emptyList());
            when(softwareBuildVersionAppService.checkReleaseGate(sbvId)).thenReturn(false);

            assertThrows(IllegalStateException.class,
                    () -> appService.createUpgradeTarget(activityId, new Long[]{sbvId}));

            verify(activityUpgradeTargetDao, never()).batchInsertPo(any());
        }

        @Test
        @DisplayName("SBV 已绑定 -> 跳过门禁校验")
        void alreadyBound_skipsGate() {
            Long activityId = 1L;
            Long sbvId = 10L;

            ActivityUpgradeTargetPo existing = new ActivityUpgradeTargetPo();
            existing.setActivityId(activityId);
            existing.setSoftwareBuildVersionId(sbvId);
            when(activityUpgradeTargetDao.selectPoByActivityId(activityId))
                    .thenReturn(java.util.List.of(existing));

            int result = appService.createUpgradeTarget(activityId, new Long[]{sbvId});

            assertEquals(0, result);
            verify(softwareBuildVersionAppService, never()).checkReleaseGate(anyLong());
        }
    }
}
