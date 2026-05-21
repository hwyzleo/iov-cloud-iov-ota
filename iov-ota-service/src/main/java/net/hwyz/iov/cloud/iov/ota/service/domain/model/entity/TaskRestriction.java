package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskRestrictionType;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

@Getter
@Builder
public class TaskRestriction {
    
    private final Long id;
    private final TaskId taskId;
    private final TaskRestrictionType type;
    private final String expression;
    
    public boolean isSatisfiedBy(VehicleInfo vehicle) {
        switch (type) {
            case BASELINE_EXCLUDE:
                return checkBaselineExclude(vehicle);
            case BASELINE_UNIFICATION:
                return checkBaselineUnification(vehicle);
            case ADAPTATION_SUBJECT:
                return checkAdaptationSubject(vehicle);
            default:
                return true;
        }
    }
    
    private boolean checkBaselineExclude(VehicleInfo vehicle) {
        String[] excludedBaselines = expression.split(",");
        for (String baseline : excludedBaselines) {
            if (baseline.equals(vehicle.getBaselineCode())) {
                return false;
            }
        }
        return true;
    }
    
    private boolean checkBaselineUnification(VehicleInfo vehicle) {
        boolean forceAlignment = Boolean.parseBoolean(expression);
        if (!forceAlignment && !vehicle.getIsBaselineAlignment()) {
            return false;
        }
        return true;
    }
    
    private boolean checkAdaptationSubject(VehicleInfo vehicle) {
        return false;
    }
}