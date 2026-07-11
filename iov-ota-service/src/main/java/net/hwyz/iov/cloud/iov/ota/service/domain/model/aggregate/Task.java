package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import cn.hutool.json.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskRestrictionType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.TaskStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskRestriction;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskRestrictionVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskStrategy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskStrategyVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleInfo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.*;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class Task {
    
    private final TaskId id;
    @Setter private String name;
    @Setter private TaskType type;
    @Setter private TaskPhase phase;
    @Setter private ActivityId activityId;
    @Setter private String target;
    @Setter private Set<Vin> vehicles;
    @Setter private Instant startTime;
    @Setter private Instant endTime;
    @Setter private Instant releaseTime;
    @Setter private String noticeType;
    @Setter private UpgradeMode upgradeMode;
    @Setter private UpgradeModeArg upgradeModeArg;
    @Setter private TaskState state;
    @Setter private String description;
    
    private Map<TaskRestrictionType, TaskRestriction> restrictions;
    private List<TaskStrategy> strategies;
    
    private final List<TaskEvent> pendingEvents = new ArrayList<>();
    
    public static Task create(TaskId id, String name, TaskType type, ActivityId activityId) {
        Task task = new Task(id);
        task.name = name;
        task.type = type;
        task.activityId = activityId;
        task.state = TaskState.PENDING;
        task.phase = TaskPhase.VALIDATION;
        task.restrictions = new HashMap<>();
        task.strategies = new ArrayList<>();
        task.pendingEvents.add(new TaskCreatedEvent(id, name, type));
        return task;
    }
    
    private Task(TaskId id) {
        this.id = id;
    }
    
    public static Task fromPo(TaskId id) {
        return new Task(id);
    }
    
    public void loadRestrictionsAndStrategies(
            List<TaskRestriction> restrictionList,
            List<TaskStrategy> strategyList) {
        this.restrictions = restrictionList.stream()
            .collect(Collectors.toMap(TaskRestriction::getType, r -> r));
        this.strategies = strategyList;
    }
    
    public void submit() {
        validateState(TaskState.PENDING, "只能在待提交状态下提交任务");
        
        this.state = TaskState.SUBMITTED;
        this.phase = TaskPhase.CANARY;
        pendingEvents.add(new TaskSubmittedEvent(this.id, this.name, this.activityId));
        
        log.info("任务[{}]已提交，当前状态[{}]", id.getValue(), state);
    }
    
    public void audit(boolean approved, String reason) {
        validateState(TaskState.SUBMITTED, "只能在已提交状态下审核任务");
        
        if (approved) {
            this.state = TaskState.APPROVED;
            pendingEvents.add(new TaskApprovedEvent(this.id, this.name));
        } else {
            this.state = TaskState.REJECTED;
            this.description = reason;
            pendingEvents.add(new TaskRejectedEvent(this.id, this.name, reason));
        }
        
        log.info("任务[{}]审核结果[{}]，当前状态[{}]", id.getValue(), approved, state);
    }
    
    public void release() {
        validateState(TaskState.APPROVED, "只能在已审核状态下发布任务");
        
        this.vehicles = parseTargetToVehicles(this.target);
        this.releaseTime = Instant.now();
        this.state = TaskState.RELEASED;
        
        pendingEvents.add(new TaskReleasedEvent(this.id, this.name, this.vehicles));
        
        log.info("任务[{}]已发布，影响车辆数量[{}]", id.getValue(), vehicles.size());
    }
    
    public void pause() {
        validateState(TaskState.RELEASED, "只能在已发布状态下暂停任务");
        
        this.state = TaskState.PAUSED;
        pendingEvents.add(new TaskPausedEvent(this.id));
        
        log.info("任务[{}]已暂停", id.getValue());
    }
    
    public void resume() {
        validateState(TaskState.PAUSED, "只能在已暂停状态下恢复任务");
        
        this.state = TaskState.RELEASED;
        pendingEvents.add(new TaskResumedEvent(this.id));
        
        log.info("任务[{}]已恢复", id.getValue());
    }
    
    public void cancel() {
        if (this.state != TaskState.RELEASED && this.state != TaskState.PAUSED) {
            throw new TaskStateException("只能在已发布或已暂停状态下取消任务");
        }
        
        this.state = TaskState.CANCELLED;
        pendingEvents.add(new TaskCancelledEvent(this.id, this.name));
        
        log.info("任务[{}]已取消", id.getValue());
    }

    /**
     * 结束任务（到达endTime或全部车辆达终态）
     * 前置守卫：state ∈ {RELEASED, PAUSED}
     * 后置动作：state -> FINISHED, 移出已发布缓存, 生成任务报告
     */
    public void finish() {
        if (this.state != TaskState.RELEASED && this.state != TaskState.PAUSED) {
            throw new TaskStateException("只能在已发布或已暂停状态下结束任务");
        }

        this.state = TaskState.FINISHED;
        pendingEvents.add(new TaskFinishedEvent(this.id, this.name));

        log.info("任务[{}]已结束", id.getValue());
    }
    
    public List<TaskEvent> getPendingEvents() {
        return new ArrayList<>(pendingEvents);
    }
    
    public void clearPendingEvents() {
        pendingEvents.clear();
    }
    
    public boolean checkPreconditions(VehicleDo vehicle) {
        if (!checkTaskTimeRange()) {
            return false;
        }
        if (!checkRestrictions(vehicle)) {
            return false;
        }
        return true;
    }
    
    private boolean checkTaskTimeRange() {
        long now = System.currentTimeMillis();
        return now >= this.startTime.toEpochMilli() && now <= this.endTime.toEpochMilli();
    }
    
    private boolean checkRestrictions(VehicleDo vehicle) {
        if (this.restrictions == null) return true;
        for (TaskRestriction restriction : this.restrictions.values()) {
            VehicleInfo vehicleInfo = VehicleInfo.builder()
                .vin(vehicle.getId())
                .baselineCode(vehicle.getBaselineCode())
                .isBaselineAlignment(vehicle.getIsBaselineAlignment())
                .build();
            if (!restriction.isSatisfiedBy(vehicleInfo)) {
                log.info("车辆[{}]不满足任务[{}]的限制条件[{}]", vehicle.getId(), this.id.getValue(), restriction.getType());
                return false;
            }
        }
        return true;
    }
    
    public Map<TaskRestrictionType, TaskRestrictionVo> getTaskRestrictionMap() {
        if (this.restrictions == null) return new HashMap<>();
        return this.restrictions.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> TaskRestrictionVo.builder()
                    .id(e.getValue().getId())
                    .taskId(e.getValue().getTaskId() != null ? e.getValue().getTaskId().getValue() : null)
                    .restrictionType(e.getValue().getType())
                    .restrictionExpression(e.getValue().getExpression())
                    .build()
            ));
    }
    
    public List<TaskStrategyVo> getTaskStrategyList() {
        if (this.strategies == null) return new ArrayList<>();
        return this.strategies.stream()
            .map(s -> TaskStrategyVo.builder()
                .id(s.getId())
                .taskId(s.getTaskId() != null ? s.getTaskId().getValue() : null)
                .strategyType(s.getType())
                .strategyExpression(s.getStrategy())
                .build())
            .collect(Collectors.toList());
    }
    
    public Date getStartTimeDate() {
        return this.startTime != null ? Date.from(this.startTime) : null;
    }
    
    public Date getEndTimeDate() {
        return this.endTime != null ? Date.from(this.endTime) : null;
    }
    
    public JSONObject getUpgradeModeArgJson() {
        if (this.upgradeModeArg == null) return new JSONObject();
        return this.upgradeModeArg.getValue();
    }
    
    private void validateState(TaskState expectedState, String message) {
        if (this.state != expectedState) {
            throw new TaskStateException(message);
        }
    }
    
    private Set<Vin> parseTargetToVehicles(String target) {
        return Arrays.stream(target.split(","))
            .map(Vin::of)
            .collect(Collectors.toSet());
    }
}