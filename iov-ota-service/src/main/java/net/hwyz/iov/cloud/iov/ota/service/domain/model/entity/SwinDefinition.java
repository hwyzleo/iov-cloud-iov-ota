package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.Instant;

/**
 * SwinDefinition 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class SwinDefinition {

    private Long id;
    private String swinCode;
    private String schemeCode;
    private String typeRefType;
    private String typeRefCode;
    private String name;
    private String status;
    private Instant syncTime;
}
