package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class TaskStrategyResult {
    
    private Long id;
    
    private String type;
    
    private String strategy;
}