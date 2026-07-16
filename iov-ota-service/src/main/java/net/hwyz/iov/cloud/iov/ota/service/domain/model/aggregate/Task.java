package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import cn.hutool.json.JSONArray;
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
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.DeviceInfoVo;
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
    @Setter private TaskState stateBeforePause; // 暂停前的状态
    @Setter private String pauseReason;  // MANUAL/GATE_HALT/RISK_HALT/COMPLIANCE_HALT
    @Setter private String pausedBy;     // HUMAN/SYSTEM
    @Setter private Boolean autoResume;  // 是否自动恢复（门禁暂停后自动恢复）
    @Setter private String cancelReason; // DISCARD/ABORT/ROLLBACK/COMPLIANCE/SUPERSEDED_BY
    @Setter private Instant actualReleaseTime;  // 实际发布时间（发布事务成功时间）
    @Setter private String lastScheduleError;   // 最近一次到点发布失败摘要
    
    private Map<TaskRestrictionType, TaskRestriction> restrictions;
    private List<TaskStrategy> strategies;
    
    private final List<TaskEvent> pendingEvents = new ArrayList<>();
    
    public static Task create(TaskId id, String name, TaskType type, ActivityId activityId) {
        Task task = new Task(id);
        task.name = name;
        task.type = type;
        task.activityId = activityId;
        task.state = TaskState.DRAFT;
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
            .collect(Collectors.toMap(TaskRestriction::getType, r -> r, (existing, replacement) -> replacement));
        this.strategies = strategyList;
    }
    
    public void submit() {
        validateState(TaskState.DRAFT, "只能在草稿状态下提交任务");
        
        this.state = TaskState.PENDING_APPROVAL;
        // phase 固定不可变，不在状态转换中修改
        pendingEvents.add(new TaskSubmittedEvent(this.id, this.name, this.activityId));
        
        log.info("任务[{}]已提交，当前状态[{}]", id.getValue(), state);
    }
    
    /**
     * 审核任务（简单审核，已废弃，请使用approve方法）
     * @deprecated 使用 {@link #approve(boolean, String)} 替代
     */
    @Deprecated
    public void audit(boolean approved, String reason) {
        validateState(TaskState.PENDING_APPROVAL, "只能在待审批状态下审核任务");
        
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
    
    /**
     * 审批任务（支持多级审批）
     * @param approved 是否通过
     * @param reason 原因（拒绝时）
     */
    public void approve(boolean approved, String reason) {
        validateState(TaskState.PENDING_APPROVAL, "只能在待审批状态下审批任务");
        
        if (approved) {
            this.state = TaskState.APPROVED;
            pendingEvents.add(new TaskApprovedEvent(this.id, this.name));
        } else {
            this.state = TaskState.REJECTED;
            this.description = reason;
            pendingEvents.add(new TaskRejectedEvent(this.id, this.name, reason));
        }
        
        log.info("任务[{}]审批结果[{}]，当前状态[{}]", id.getValue(), approved, state);
    }
    
    /**
     * 统一发布任务（立即发布或到点发布）
     * 前置守卫：state ∈ {APPROVED, SCHEDULED}
     * 后置动作：state -> RELEASED, 设置actualReleaseTime
     * @param vehicles 车辆集合
     * @param trigger 触发方式：IMMEDIATE/SCHEDULER
     */
    public void release(Set<Vin> vehicles, String trigger) {
        if (this.state != TaskState.APPROVED && this.state != TaskState.SCHEDULED) {
            throw new TaskStateException("只能在已审批或已排程状态下发布任务");
        }
        
        // 校验时间约束：now < endTime
        Instant now = Instant.now();
        if (this.endTime != null && !now.isBefore(this.endTime)) {
            throw new TaskStateException("已超过任务结束时间，无法发布");
        }
        
        // 如果是定时触发，需要校验releaseTime
        if ("SCHEDULER".equals(trigger)) {
            if (this.state != TaskState.SCHEDULED) {
                throw new TaskStateException("定时发布只能在已排程状态下执行");
            }
            if (this.releaseTime != null && now.isBefore(this.releaseTime)) {
                throw new TaskStateException("尚未到达计划发布时间");
            }
        }
        
        // 如果是立即发布，将releaseTime设置为当前时间
        if ("IMMEDIATE".equals(trigger)) {
            this.releaseTime = now;
        }
        
        this.vehicles = vehicles;
        this.actualReleaseTime = now;
        this.state = TaskState.RELEASED;
        
        pendingEvents.add(new TaskReleasedEvent(this.id, this.name, this.vehicles));
        
        log.info("任务[{}]已发布，触发方式[{}]，影响车辆数量[{}]", id.getValue(), trigger, vehicles.size());
    }
    
    /**
     * 激活放量（首批放量或首车领取）
     * 前置守卫：state ∈ {RELEASED}
     * 后置动作：state -> IN_PROGRESS
     */
    public void activateRollout() {
        validateState(TaskState.RELEASED, "只能在已发布状态下激活放量");
        
        // 校验是否到达开始时间
        Instant now = Instant.now();
        if (this.startTime != null && now.isBefore(this.startTime)) {
            throw new TaskStateException("尚未到达任务开始时间，无法激活放量");
        }
        
        this.state = TaskState.IN_PROGRESS;
        pendingEvents.add(new TaskActivatedEvent(this.id, this.name));
        
        log.info("任务[{}]已激活放量，状态变更为IN_PROGRESS", id.getValue());
    }
    
    /**
     * 排程任务（定时发布）
     * 前置守卫：state ∈ {APPROVED}
     * 后置动作：state -> SCHEDULED, 设置releaseTime
     * @param releaseTime 计划发布时间
     */
    public void schedule(Instant releaseTime) {
        validateState(TaskState.APPROVED, "只能在已审批状态下排程任务");
        
        // 校验时间约束：now < releaseTime <= startTime < endTime
        Instant now = Instant.now();
        if (!now.isBefore(releaseTime)) {
            throw new TaskStateException("定时发布时间必须大于当前时间");
        }
        if (this.startTime != null && releaseTime.isAfter(this.startTime)) {
            throw new TaskStateException("定时发布时间不能晚于任务开始时间");
        }
        
        this.releaseTime = releaseTime;
        this.state = TaskState.SCHEDULED;
        pendingEvents.add(new TaskScheduledEvent(this.id, this.name, releaseTime));
        
        log.info("任务[{}]已排程，计划发布时间[{}]", id.getValue(), releaseTime);
    }
    
    /**
     * 取消排程
     * 前置守卫：state ∈ {SCHEDULED}
     * 后置动作：state -> APPROVED, 清除releaseTime
     */
    public void unschedule() {
        validateState(TaskState.SCHEDULED, "只能在已排程状态下取消排程");
        
        this.state = TaskState.APPROVED;
        this.releaseTime = null;
        pendingEvents.add(new TaskUnscheduledEvent(this.id, this.name));
        
        log.info("任务[{}]已取消排程", id.getValue());
    }
    
    public void pause() {
        if (this.state != TaskState.RELEASED && this.state != TaskState.IN_PROGRESS) {
            throw new TaskStateException("只能在已发布或执行中状态下暂停任务");
        }
        
        this.stateBeforePause = this.state;
        this.state = TaskState.PAUSED;
        pendingEvents.add(new TaskPausedEvent(this.id));
        
        log.info("任务[{}]已暂停，暂停前状态[{}]", id.getValue(), stateBeforePause);
    }
    
    /**
     * 暂停任务（带原因）
     * 前置守卫：state ∈ {RELEASED, IN_PROGRESS}
     * 后置动作：state -> PAUSED, 记录暂停原因
     */
    public void pause(String pauseReason, String pausedBy) {
        if (this.state != TaskState.RELEASED && this.state != TaskState.IN_PROGRESS) {
            throw new TaskStateException("只能在已发布或执行中状态下暂停任务");
        }
        
        this.stateBeforePause = this.state;
        this.state = TaskState.PAUSED;
        this.pauseReason = pauseReason;
        this.pausedBy = pausedBy;
        pendingEvents.add(new TaskPausedEvent(this.id));
        
        log.info("任务[{}]已暂停，原因[{}]，发起方[{}]，暂停前状态[{}]", id.getValue(), pauseReason, pausedBy, stateBeforePause);
    }
    
    public void resume() {
        validateState(TaskState.PAUSED, "只能在已暂停状态下恢复任务");
        
        // 恢复到暂停前的状态
        if (this.stateBeforePause != null) {
            this.state = this.stateBeforePause;
        } else {
            // 兼容旧数据：如果没有记录暂停前状态，使用原来的逻辑
            if (this.actualReleaseTime != null && this.startTime != null && 
                !Instant.now().isBefore(this.startTime)) {
                this.state = TaskState.IN_PROGRESS;
            } else {
                this.state = TaskState.RELEASED;
            }
        }
        this.stateBeforePause = null;
        pendingEvents.add(new TaskResumedEvent(this.id));
        
        log.info("任务[{}]已恢复，当前状态[{}]", id.getValue(), state);
    }
    
    public void cancel() {
        if (this.state != TaskState.APPROVED && this.state != TaskState.SCHEDULED && 
            this.state != TaskState.RELEASED && this.state != TaskState.IN_PROGRESS && 
            this.state != TaskState.PAUSED) {
            throw new TaskStateException("只能在已审批、已排程、已发布、执行中或已暂停状态下取消任务");
        }
        
        this.state = TaskState.CANCELED;
        pendingEvents.add(new TaskCancelledEvent(this.id, this.name));
        
        log.info("任务[{}]已取消", id.getValue());
    }
    
    /**
     * 取消任务（带原因）
     * 前置守卫：state ∈ {APPROVED, SCHEDULED, RELEASED, IN_PROGRESS, PAUSED}
     * 后置动作：state -> CANCELED, 记录取消原因
     */
    public void cancel(String cancelReason) {
        if (this.state != TaskState.APPROVED && this.state != TaskState.SCHEDULED && 
            this.state != TaskState.RELEASED && this.state != TaskState.IN_PROGRESS && 
            this.state != TaskState.PAUSED) {
            throw new TaskStateException("只能在已审批、已排程、已发布、执行中或已暂停状态下取消任务");
        }
        
        this.state = TaskState.CANCELED;
        this.cancelReason = cancelReason;
        pendingEvents.add(new TaskCancelledEvent(this.id, this.name));
        
        log.info("任务[{}]已取消，原因[{}]", id.getValue(), cancelReason);
    }

    /**
     * 结束任务（到达endTime或全部车辆达终态）
     * 前置守卫：state ∈ {IN_PROGRESS, PAUSED}
     * 后置动作：state -> COMPLETED, 移出已发布缓存, 生成任务报告
     */
    public void finish() {
        if (this.state != TaskState.IN_PROGRESS && this.state != TaskState.PAUSED) {
            throw new TaskStateException("只能在执行中或已暂停状态下结束任务");
        }

        this.state = TaskState.COMPLETED;
        pendingEvents.add(new TaskFinishedEvent(this.id, this.name));

        log.info("任务[{}]已结束", id.getValue());
    }
    
    /**
     * 取代任务（被新任务取代）
     * 前置守卫：state ∈ {RELEASED, IN_PROGRESS, PAUSED}
     * 后置动作：state -> SUPERSEDED
     */
    public void supersede() {
        if (this.state != TaskState.RELEASED && this.state != TaskState.IN_PROGRESS && 
            this.state != TaskState.PAUSED) {
            throw new TaskStateException("只能在已发布、执行中或已暂停状态下取代任务");
        }
        
        this.state = TaskState.SUPERSEDED;
        pendingEvents.add(new TaskSupersededEvent(this.id, this.name));
        
        log.info("任务[{}]已被取代", id.getValue());
    }
    
    public List<TaskEvent> getPendingEvents() {
        return new ArrayList<>(pendingEvents);
    }
    
    public void clearPendingEvents() {
        pendingEvents.clear();
    }
    
    /**
     * 检查车端前置条件
     * 车端check必须满足：state=IN_PROGRESS AND start_time<=now<end_time AND admit_state=PASS
     */
    public boolean checkPreconditions(VehicleDo vehicle) {
        if (!checkTaskStateForVehicle()) {
            return false;
        }
        if (!checkTaskTimeRange()) {
            return false;
        }
        if (!checkRestrictions(vehicle)) {
            return false;
        }
        return true;
    }
    
    /**
     * 检查任务状态是否允许车端领取
     * 只有IN_PROGRESS状态才允许车端领取任务
     */
    private boolean checkTaskStateForVehicle() {
        // 车端只能在IN_PROGRESS状态下领取任务
        if (this.state != TaskState.IN_PROGRESS) {
            log.debug("任务[{}]状态[{}]不允许车端领取", id.getValue(), state);
            return false;
        }
        return true;
    }
    
    /**
     * 检查任务时间范围
     * 车端check必须满足：start_time<=now<end_time
     */
    private boolean checkTaskTimeRange() {
        long now = System.currentTimeMillis();
        // 检查是否在执行窗口内
        if (this.startTime != null && now < this.startTime.toEpochMilli()) {
            log.debug("任务[{}]尚未到达开始时间[{}]", id.getValue(), startTime);
            return false;
        }
        if (this.endTime != null && now >= this.endTime.toEpochMilli()) {
            log.debug("任务[{}]已超过结束时间[{}]", id.getValue(), endTime);
            return false;
        }
        return true;
    }
    
    /**
     * 检查任务是否已发布但未到开始时间
     * 用于后台展示"已发布，等待执行窗口"
     */
    public boolean isReleasedButNotStarted() {
        if (this.state != TaskState.RELEASED) {
            return false;
        }
        Instant now = Instant.now();
        return this.startTime != null && now.isBefore(this.startTime);
    }
    
    /**
     * 检查任务是否可以激活放量
     * 条件：state=RELEASED AND now>=startTime
     */
    public boolean canActivateRollout() {
        if (this.state != TaskState.RELEASED) {
            return false;
        }
        Instant now = Instant.now();
        return this.startTime == null || !now.isBefore(this.startTime);
    }
    
    private boolean checkRestrictions(VehicleDo vehicle) {
        if (this.restrictions == null) return true;
        for (TaskRestriction restriction : this.restrictions.values()) {
            // 构建VehicleInfo，包含扩展字段用于条件分层建模
            VehicleInfo.VehicleInfoBuilder builder = VehicleInfo.builder()
                .vin(vehicle.getId())
                .baselineCode(vehicle.getBaselineCode())
                .isBaselineAlignment(vehicle.getIsBaselineAlignment());
            
            // 从车辆设备信息中提取扩展字段
            if (vehicle.getDeviceMap() != null && !vehicle.getDeviceMap().isEmpty()) {
                // 取第一个设备作为主要设备
                DeviceInfoVo deviceInfo = vehicle.getDeviceMap().values().iterator().next();
                builder.adaptationSubject(deviceInfo.getSoftwarePn())  // 适配主体使用软件零件号
                       .softwareVersion(deviceInfo.getSoftwarePartVer())
                       .hardwareVersion(deviceInfo.getHardwarePartVer())
                       .deviceCode(deviceInfo.getDeviceCode())
                       .softwarePn(deviceInfo.getSoftwarePn())
                       .hardwarePn(deviceInfo.getHardwarePn());
            }
            
            VehicleInfo vehicleInfo = builder.build();
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
                    .restrictionLevel(e.getValue().getRestrictionLevel())
                    .errorMessage(e.getValue().getErrorMessage())
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
                .downloadRetryMax(s.getDownloadRetryMax())
                .retryBackoff(s.getRetryBackoff())
                .resumeOnPoweroff(s.getResumeOnPoweroff())
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
        // 支持结构化目标定义（US-056/057）
        // 目标格式：JSON字符串，包含mode字段
        // mode=LIST: vins数组
        // mode=IMPORT: 文件导入（暂不实现）
        // mode=CONDITION: 条件表达式（暂不实现）
        try {
            JSONObject json = new JSONObject(target);
            String mode = json.getStr("mode");
            if (mode != null) {
                return switch (mode) {
                    case "LIST" -> {
                        List<String> vins = json.getJSONArray("vins").toList(String.class);
                        yield vins.stream().map(Vin::of).collect(Collectors.toSet());
                    }
                    case "IMPORT" -> {
                        // 文件导入VIN列表
                        // 支持两种格式：
                        // 1. 直接包含vins数组：{"mode":"IMPORT","vins":["VIN001","VIN002"]}
                        // 2. 包含fileId，需要从文件服务解析（暂未实现）
                        if (json.containsKey("vins")) {
                            List<String> vins = json.getJSONArray("vins").toList(String.class);
                            yield vins.stream().map(Vin::of).collect(Collectors.toSet());
                        } else if (json.containsKey("fileId")) {
                            // TODO: 从文件服务解析VIN列表
                            log.warn("IMPORT模式文件解析暂未实现，fileId: {}", json.getStr("fileId"));
                            yield new HashSet<>();
                        } else {
                            log.warn("IMPORT模式缺少vins或fileId字段");
                            yield new HashSet<>();
                        }
                    }
                    case "CONDITION" -> {
                        // 条件表达式查询车辆
                        // 预期格式：{"mode":"CONDITION","logic":"AND","conditions":[{"field":"baselineCode","operator":"=","value":"BL001"}]}
                        // 由 TargetResolutionDomainService 处理，此处仅做格式验证
                        JSONArray conditions = json.getJSONArray("conditions");
                        if (conditions == null || conditions.isEmpty()) {
                            log.warn("CONDITION模式缺少conditions数组");
                            yield new HashSet<>();
                        }
                        log.info("CONDITION模式目标定义，条件数[{}]，将由TargetResolutionDomainService解析", conditions.size());
                        yield new HashSet<>();
                    }
                    default -> throw new IllegalArgumentException("未知的目标模式: " + mode);
                };
            }
        } catch (Exception e) {
            // 兼容旧格式：逗号分隔的VIN列表
            log.debug("目标不是JSON格式，尝试旧格式解析: {}", e.getMessage());
        }
        
        // 旧格式：逗号分隔的VIN列表
        return Arrays.stream(target.split(","))
            .map(Vin::of)
            .collect(Collectors.toSet());
    }
}