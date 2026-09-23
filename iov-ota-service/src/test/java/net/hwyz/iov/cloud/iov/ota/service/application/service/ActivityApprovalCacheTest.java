package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityApprovalMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityApprovalPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("审批/型批评估直写 DB 后失效活动内存缓存")
class ActivityApprovalCacheTest {

    @Mock private ActivityMapper activityDao;
    @Mock private ActivityApprovalMapper activityApprovalDao;
    @Mock private CacheService cacheService;

    @InjectMocks
    private ActivityAppService appService;

    private ActivityApprovalPo approvalPo(String stage, String result) {
        ActivityApprovalPo po = new ActivityApprovalPo();
        po.setApprovalStage(stage);
        po.setResult(result);
        return po;
    }

    @Test
    @DisplayName("三级审批全部通过 -> 状态跃迁已审核并失效缓存（发布可读到新状态）")
    void approveAllPassed_evictsCache() {
        Long activityId = 1L;

        ActivityPo activity = new ActivityPo();
        activity.setId(activityId);
        activity.setState(ActivityState.SUBMITTED.value);
        when(activityDao.selectPoById(activityId)).thenReturn(activity);
        when(activityApprovalDao.selectByActivityId(activityId)).thenReturn(List.of(
                approvalPo("QUALITY", "PASS"),
                approvalPo("PRODUCT", "PASS"),
                approvalPo("SECURITY", "PASS")
        ));

        appService.approveActivity(activityId, "SECURITY", "1", "PASS", "同意");

        verify(activityDao).updatePo(argThat(po -> po.getState() == ActivityState.APPROVED.value));
        verify(cacheService).removeActivity(activityId);
    }

    @Test
    @DisplayName("任一级驳回 -> 回到未通过并失效缓存")
    void approveRejected_evictsCache() {
        Long activityId = 1L;

        ActivityPo activity = new ActivityPo();
        activity.setId(activityId);
        activity.setState(ActivityState.SUBMITTED.value);
        when(activityDao.selectPoById(activityId)).thenReturn(activity);

        appService.approveActivity(activityId, "QUALITY", "1", "REJECT", "不合格");

        verify(activityDao).updatePo(argThat(po -> po.getState() == ActivityState.REJECTED.value));
        verify(cacheService).removeActivity(activityId);
    }

    @Test
    @DisplayName("修改活动直写 DB 后失效缓存")
    void modifyActivity_evictsCache() {
        Long activityId = 1L;

        ActivityPo activity = new ActivityPo();
        activity.setId(activityId);
        when(activityDao.updatePo(any())).thenReturn(1);

        appService.modifyActivity(activity);

        verify(cacheService).removeActivity(activityId);
        assertEquals(1, activityDao.updatePo(activity));
    }
}
