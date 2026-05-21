package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;
import lombok.Builder;

import java.time.Instant;

@Data
@Builder
public class TaskQuery {
    
    private Long activityId;
    
    private String state;
    
    private String type;
    
    private String name;
    
    private Instant startTimeBegin;
    
    private Instant startTimeEnd;
}