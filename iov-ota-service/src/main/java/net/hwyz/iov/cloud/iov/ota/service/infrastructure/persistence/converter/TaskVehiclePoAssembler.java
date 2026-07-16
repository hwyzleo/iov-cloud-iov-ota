package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 升级任务车辆数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskVehiclePoAssembler {

    TaskVehiclePoAssembler INSTANCE = Mappers.getMapper(TaskVehiclePoAssembler.class);

    /**
     * 数据对象转领域对象
     *
     * @param taskVehiclePo 数据对象
     * @return 领域对象
     */
    @Mappings({
            @Mapping(target = "state", ignore = true),
            @Mapping(target = "baselineCode", ignore = true),
            @Mapping(target = "activityVersion", ignore = true),
            @Mapping(target = "activityReleaseTime", ignore = true),
            @Mapping(target = "upgradePurpose", ignore = true),
            @Mapping(target = "upgradeFunction", ignore = true),
            @Mapping(target = "activityStatement", ignore = true),
            @Mapping(target = "taskStartTime", ignore = true),
            @Mapping(target = "taskEndTime", ignore = true),
            @Mapping(target = "upgradeMode", ignore = true),
            @Mapping(target = "upgradeModeArg", ignore = true),
            @Mapping(target = "strategyMap", ignore = true),
            @Mapping(target = "softwareBuildVersionList", ignore = true),
            @Mapping(target = "compatiblePnMap", ignore = true),
            @Mapping(target = "upgradeNoticeArticleId", ignore = true),
            @Mapping(target = "activityTermArticleId", ignore = true),
            @Mapping(target = "privacyAgreementArticleId", ignore = true),
            @Mapping(target = "taskState", ignore = true),
            @Mapping(target = "nextRetryAt", expression = "java(toInstant(taskVehiclePo.getNextRetryAt()))")
    })
    TaskVehicleDo toDo(TaskVehiclePo taskVehiclePo);

    /**
     * 领域对象转数据对象
     *
     * @param taskVehicleDo 领域对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "state", ignore = true),
            @Mapping(target = "activityId", ignore = true),
            @Mapping(target = "taskId", ignore = true),
            @Mapping(target = "vin", ignore = true),
            @Mapping(target = "resultCode", ignore = true),
            @Mapping(target = "description", ignore = true),
            @Mapping(target = "createBy", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "modifyBy", ignore = true),
            @Mapping(target = "modifyTime", ignore = true),
            @Mapping(target = "rowVersion", ignore = true),
            @Mapping(target = "rowValid", ignore = true),
            @Mapping(target = "nextRetryAt", expression = "java(toLocalDateTime(taskVehicleDo.getNextRetryAt()))")
    })
    TaskVehiclePo fromDo(TaskVehicleDo taskVehicleDo);

    default Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
