package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivitySoftwareBuildVersionPo;
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

/**
 * 升级活动应用服务测试 (CR-004 双重门禁)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityAppService 双重发布门禁")
class ActivityAppServiceTest {

    @Mock private ActivityMapper activityDao;
    @Mock private ActivityCompatiblePnMapper activityCompatiblePnDao;
    @Mock private ActivityFixedConfigWordMapper activityFixedConfigWordDao;
    @Mock private ActivitySoftwareBuildVersionMapper activitySoftwareBuildVersionDao;
    @Mock private ActivityTargetVersionMapper activityTargetVersionDao;
    @Mock private SoftwareBuildVersionAppService softwareBuildVersionAppService;

    @InjectMocks
    private ActivityAppService appService;

    @Nested
    @DisplayName("createSoftwareBuildVersion - 门禁校验")
    class CreateSbvWithGate {

        @Test
        @DisplayName("SBV 通过门禁 -> 绑定成功")
        void gatePassed_bindsSbv() {
            Long activityId = 1L;
            Long sbvId = 10L;

            when(activitySoftwareBuildVersionDao.selectPoByActivityId(activityId))
                    .thenReturn(Collections.emptyList());
            when(softwareBuildVersionAppService.checkReleaseGate(sbvId)).thenReturn(true);
            when(activitySoftwareBuildVersionDao.batchInsertPo(any())).thenReturn(1);

            int result = appService.createSoftwareBuildVersion(activityId, new Long[]{sbvId});

            assertEquals(1, result);
            verify(softwareBuildVersionAppService).checkReleaseGate(sbvId);
        }

        @Test
        @DisplayName("SBV 未通过门禁 -> 阻断绑定")
        void gateFailed_blocksBinding() {
            Long activityId = 1L;
            Long sbvId = 10L;

            when(activitySoftwareBuildVersionDao.selectPoByActivityId(activityId))
                    .thenReturn(Collections.emptyList());
            when(softwareBuildVersionAppService.checkReleaseGate(sbvId)).thenReturn(false);

            assertThrows(IllegalStateException.class,
                    () -> appService.createSoftwareBuildVersion(activityId, new Long[]{sbvId}));

            verify(activitySoftwareBuildVersionDao, never()).batchInsertPo(any());
        }

        @Test
        @DisplayName("SBV 已绑定 -> 跳过门禁校验")
        void alreadyBound_skipsGate() {
            Long activityId = 1L;
            Long sbvId = 10L;

            ActivitySoftwareBuildVersionPo existing = new ActivitySoftwareBuildVersionPo();
            existing.setActivityId(activityId);
            existing.setSoftwareBuildVersionId(sbvId);
            when(activitySoftwareBuildVersionDao.selectPoByActivityId(activityId))
                    .thenReturn(java.util.List.of(existing));

            int result = appService.createSoftwareBuildVersion(activityId, new Long[]{sbvId});

            assertEquals(0, result);
            verify(softwareBuildVersionAppService, never()).checkReleaseGate(anyLong());
        }
    }
}
