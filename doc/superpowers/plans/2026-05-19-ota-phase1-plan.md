# OTA服务DDD架构重构 - 第一阶段实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建Domain层核心架构（Domain Model、Repository接口、Domain Service）和Infrastructure层的Repository实现，不破坏现有功能。

**Architecture:** 
- Domain层包含Model（聚合根、实体、值对象）、Repository接口定义、Domain Service（纯Java类）
- Infrastructure层实现Repository接口，委托给现有Mapper
- Application层保持不变，仅引入Repository替代部分Mapper调用（可选）

**Tech Stack:** 
- Java 17
- Spring Boot
- MyBatis-Plus（Mapper委托）
- MapStruct（Converter）
- Lombok

---

## File Structure

### 新增文件

**Domain层（约15个文件）**

| 文件路径 | 职责 |
|---------|------|
| `domain/model/aggregate/SoftwareBuildVersion.java` | 软件内部版本聚合根 |
| `domain/model/entity/SoftwarePackage.java` | 软件包实体 |
| `domain/model/entity/SoftwareBuildVersionDependency.java` | 软件版本依赖实体 |
| `domain/model/entity/CompatiblePn.java` | 兼件号实体 |
| `domain/model/entity/VehiclePart.java` | 车辆零件实体 |
| `domain/model/valueobject/SoftwareBuildVersionId.java` | 软件版本ID值对象 |
| `domain/model/valueobject/SoftwarePackageId.java` | 软件包ID值对象 |
| `domain/model/valueobject/DeviceCode.java` | 设备代码值对象 |
| `domain/model/valueobject/SoftwarePn.java` | 软件零件号值对象 |
| `domain/repository/SoftwareBuildVersionRepository.java` | 软件版本Repository接口 |
| `domain/repository/SoftwarePackageRepository.java` | 软件包Repository接口 |
| `domain/repository/CompatiblePnRepository.java` | 兼件号Repository接口 |
| `domain/repository/VehiclePartRepository.java` | 车辆零件Repository接口 |
| `domain/service/SoftwareBuildVersionDomainService.java` | 软件版本领域服务（第一阶段：空实现） |
| `domain/exception/PotaBaseException.java` | 迁移现有异常 |

**Infrastructure层（约9个文件）**

| 文件路径 | 职责 |
|---------|------|
| `infrastructure/persistence/repository/SoftwareBuildVersionRepositoryImpl.java` | Repository实现 |
| `infrastructure/persistence/repository/SoftwarePackageRepositoryImpl.java` | Repository实现 |
| `infrastructure/persistence/repository/CompatiblePnRepositoryImpl.java` | Repository实现 |
| `infrastructure/persistence/repository/VehiclePartRepositoryImpl.java` | Repository实现 |
| `infrastructure/persistence/converter/SoftwareBuildVersionConverter.java` | Domain ⇄ Po转换 |
| `infrastructure/persistence/converter/SoftwarePackageConverter.java` | Domain ⇄ Po转换 |
| `infrastructure/persistence/converter/SoftwareBuildVersionDependencyConverter.java` | Domain ⇄ Po转换 |
| `infrastructure/persistence/converter/CompatiblePnConverter.java` | Domain ⇄ Po转换 |
| `infrastructure/persistence/converter/VehiclePartConverter.java` | Domain ⇄ Po转换（重命名现有） |

---

## Task 1: 创建Domain层目录结构

**Files:**
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/` (目录)

- [ ] **Step 1: 创建domain包目录**

```bash
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/aggregate
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/valueobject
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/event
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/service
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/gateway
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/policy
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/factory
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/exception
```

- [ ] **Step 2: 验证目录创建成功**

Run: `ls -la iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/`

Expected: 显示10个子目录

---

## Task 2: 创建Infrastructure层Repository目录

**Files:**
- Create: `infrastructure/persistence/repository/` (目录)

- [ ] **Step 1: 创建repository目录**

```bash
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository
```

- [ ] **Step 2: 验证目录创建成功**

Run: `ls -la iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/`

Expected: 显示repository目录

---

## Task 3: 创建值对象（ValueObject）

**Files:**
- Create: `domain/model/valueobject/*.java` (4个文件)

- [ ] **Step 1: 创建SoftwareBuildVersionId值对象**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/valueobject/SoftwareBuildVersionId.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 软件内部版本ID值对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoftwareBuildVersionId implements Serializable {
    private Long value;
}
```

- [ ] **Step 2: 创建SoftwarePackageId值对象**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/valueobject/SoftwarePackageId.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 软件包ID值对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoftwarePackageId implements Serializable {
    private Long value;
}
```

- [ ] **Step 3: 创建DeviceCode值对象**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/valueobject/DeviceCode.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备代码值对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceCode implements Serializable {
    private String value;
}
```

- [ ] **Step 4: 创建SoftwarePn值对象**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/valueobject/SoftwarePn.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model/valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 软件零件号值对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoftwarePn implements Serializable {
    private String value;
}
```

- [ ] **Step 5: 编译验证**

Run: `/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home/bin/java -version && cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/valueobject/
git commit -m "feat(domain): add value objects for domain model
- SoftwareBuildVersionId
- SoftwarePackageId
- DeviceCode
- SoftwarePn"
```

---

## Task 4: 创建实体（Entity）

**Files:**
- Create: `domain/model/entity/*.java` (4个文件)

- [ ] **Step 1: 创建SoftwarePackage实体**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwarePackage.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;

import java.io.Serializable;

/**
 * 软件包实体
 */
@Data
@Builder
public class SoftwarePackage implements Serializable {
    private SoftwarePackageId id;
    private DeviceCode deviceCode;
    private SoftwarePn softwarePn;
    private String packageCode;
    private String packageName;
    private String packageType;
    private String packageUrl;
    private String packageMd5;
    private Long packageSize;
    private String packageDesc;
}
```

- [ ] **Step 2: 创建SoftwareBuildVersionDependency实体**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwareBuildVersionDependency.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;

import java.io.Serializable;

/**
 * 软件内部版本依赖实体
 */
@Data
@Builder
public class SoftwareBuildVersionDependency implements Serializable {
    private Long id;
    private SoftwareBuildVersionId softwareBuildVersionId;
    private SoftwareBuildVersionId dependencySoftwareBuildVersionId;
    private Integer adaptiveLevel;
    private Integer sort;
}
```

- [ ] **Step 3: 创建CompatiblePn实体**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/CompatiblePn.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 兼件号实体
 */
@Data
@Builder
public class CompatiblePn implements Serializable {
    private Long id;
    private String partCode;
    private String compatiblePn;
    private String partName;
    private String description;
    private Integer status;
    private Instant createTime;
}
```

- [ ] **Step 4: 创建VehiclePart实体**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/VehiclePart.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 车辆零件实体
 */
@Data
@Builder
public class VehiclePart implements Serializable {
    private Long id;
    private String vehicleModelCode;
    private String partCode;
    private String partPn;
    private String partName;
    private String partType;
    private String description;
    private Integer status;
    private Instant createTime;
}
```

- [ ] **Step 5: 编译验证**

Run: `cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/
git commit -m "feat(domain): add entity classes
- SoftwarePackage
- SoftwareBuildVersionDependency
- CompatiblePn
- VehiclePart"
```

---

## Task 5: 创建聚合根（Aggregate）

**Files:**
- Create: `domain/model/aggregate/SoftwareBuildVersion.java`

- [ ] **Step 1: 创建SoftwareBuildVersion聚合根**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/aggregate/SoftwareBuildVersion.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import lombok.Builder;
import lombok.Data;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwareBuildVersionDependency;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 软件内部版本聚合根
 */
@Data
@Builder
public class SoftwareBuildVersion implements Serializable {
    private SoftwareBuildVersionId id;
    private DeviceCode deviceCode;
    private SoftwarePn softwarePn;
    private String softwareBuildVer;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private String adaptiveAssemblyPn;
    private String adaptiveSoftwarePn;
    private Instant releaseDate;
    
    @Builder.Default
    private List<SoftwarePackage> packages = new ArrayList<>();
    
    @Builder.Default
    private List<SoftwareBuildVersionDependency> dependencies = new ArrayList<>();
}
```

- [ ] **Step 2: 编译验证**

Run: `cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/aggregate/SoftwareBuildVersion.java
git commit -m "feat(domain): add SoftwareBuildVersion aggregate root"
```

---

## Task 6: 创建Repository接口

**Files:**
- Create: `domain/repository/*.java` (4个文件)

- [ ] **Step 1: 创建SoftwareBuildVersionRepository接口**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/SoftwareBuildVersionRepository.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;

import java.util.List;
import java.util.Map;

/**
 * 软件内部版本Repository接口
 */
public interface SoftwareBuildVersionRepository {
    
    SoftwareBuildVersion findById(SoftwareBuildVersionId id);
    
    SoftwareBuildVersion findByDeviceCodeAndPnAndVersion(DeviceCode deviceCode, SoftwarePn pn, String version);
    
    List<SoftwareBuildVersion> search(Map<String, Object> params);
    
    List<SoftwareBuildVersion> listByBaselineCode(String baselineCode);
    
    void save(SoftwareBuildVersion version);
    
    void deleteByIds(List<SoftwareBuildVersionId> ids);
    
    int countPackages(SoftwareBuildVersionId id);
    
    int countDependencies(SoftwareBuildVersionId id);
}
```

- [ ] **Step 2: 创建SoftwarePackageRepository接口**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/SoftwarePackageRepository.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;

import java.util.List;
import java.util.Map;

/**
 * 软件包Repository接口
 */
public interface SoftwarePackageRepository {
    
    SoftwarePackage findById(SoftwarePackageId id);
    
    List<SoftwarePackage> findByIds(List<SoftwarePackageId> ids);
    
    List<SoftwarePackage> search(Map<String, Object> params);
    
    void save(SoftwarePackage package);
    
    void deleteByIds(List<SoftwarePackageId> ids);
}
```

- [ ] **Step 3: 创建CompatiblePnRepository接口**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/CompatiblePnRepository.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CompatiblePn;

import java.util.List;
import java.util.Map;

/**
 * 兼件号Repository接口
 */
public interface CompatiblePnRepository {
    
    CompatiblePn findById(Long id);
    
    List<CompatiblePn> search(Map<String, Object> params);
    
    void save(CompatiblePn compatiblePn);
    
    void deleteByIds(List<Long> ids);
}
```

- [ ] **Step 4: 创建VehiclePartRepository接口**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/VehiclePartRepository.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehiclePart;

import java.util.List;
import java.util.Map;

/**
 * 车辆零件Repository接口
 */
public interface VehiclePartRepository {
    
    VehiclePart findById(Long id);
    
    List<VehiclePart> search(Map<String, Object> params);
    
    void save(VehiclePart vehiclePart);
    
    void deleteByIds(List<Long> ids);
}
```

- [ ] **Step 5: 编译验证**

Run: `cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/
git commit -m "feat(domain): add repository interfaces
- SoftwareBuildVersionRepository
- SoftwarePackageRepository
- CompatiblePnRepository
- VehiclePartRepository"
```

---

## Task 7: 创建Domain层Converter

**Files:**
- Create: `infrastructure/persistence/converter/*.java` (5个文件)

- [ ] **Step 1: 创建SoftwareBuildVersionConverter**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/SoftwareBuildVersionConverter.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件内部版本Domain Model ⇄ Po转换器
 */
@Mapper
public interface SoftwareBuildVersionConverter {
    
    SoftwareBuildVersionConverter INSTANCE = Mappers.getMapper(SoftwareBuildVersionConverter.class);
    
    @Mapping(target = "id", expression = "java(new SoftwareBuildVersionId(po.getId()))")
    @Mapping(target = "deviceCode", expression = "java(new DeviceCode(po.getDeviceCode()))")
    @Mapping(target = "softwarePn", expression = "java(new SoftwarePn(po.getSoftwarePn()))")
    SoftwareBuildVersion toDomain(SoftwareBuildVersionPo po);
    
    @Mapping(target = "id", expression = "java(domain.getId() != null ? domain.getId().getValue() : null)")
    @Mapping(target = "deviceCode", expression = "java(domain.getDeviceCode() != null ? domain.getDeviceCode().getValue() : null)")
    @Mapping(target = "softwarePn", expression = "java(domain.getSoftwarePn() != null ? domain.getSoftwarePn().getValue() : null)")
    SoftwareBuildVersionPo toPo(SoftwareBuildVersion domain);
    
    List<SoftwareBuildVersion> toDomainList(List<SoftwareBuildVersionPo> poList);
    
    List<SoftwareBuildVersionPo> toPoList(List<SoftwareBuildVersion> domainList);
}
```

- [ ] **Step 2: 创建SoftwarePackageConverter**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/SoftwarePackageConverter.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件包Domain Model ⇄ Po转换器
 */
@Mapper
public interface SoftwarePackageConverter {
    
    SoftwarePackageConverter INSTANCE = Mappers.getMapper(SoftwarePackageConverter.class);
    
    @Mapping(target = "id", expression = "java(new SoftwarePackageId(po.getId()))")
    @Mapping(target = "deviceCode", expression = "java(new DeviceCode(po.getDeviceCode()))")
    @Mapping(target = "softwarePn", expression = "java(new SoftwarePn(po.getSoftwarePn()))")
    SoftwarePackage toDomain(SoftwarePackagePo po);
    
    @Mapping(target = "id", expression = "java(domain.getId() != null ? domain.getId().getValue() : null)")
    @Mapping(target = "deviceCode", expression = "java(domain.getDeviceCode() != null ? domain.getDeviceCode().getValue() : null)")
    @Mapping(target = "softwarePn", expression = "java(domain.getSoftwarePn() != null ? domain.getSoftwarePn().getValue() : null)")
    SoftwarePackagePo toPo(SoftwarePackage domain);
    
    List<SoftwarePackage> toDomainList(List<SoftwarePackagePo> poList);
    
    List<SoftwarePackagePo> toPoList(List<SoftwarePackage> domainList);
}
```

- [ ] **Step 3: 创建SoftwareBuildVersionDependencyConverter**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/SoftwareBuildVersionDependencyConverter.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwareBuildVersionDependency;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionDependencyPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件内部版本依赖Domain Model ⇄ Po转换器
 */
@Mapper
public interface SoftwareBuildVersionDependencyConverter {
    
    SoftwareBuildVersionDependencyConverter INSTANCE = Mappers.getMapper(SoftwareBuildVersionDependencyConverter.class);
    
    @Mapping(target = "softwareBuildVersionId", expression = "java(new SoftwareBuildVersionId(po.getSoftwareBuildVersionId()))")
    @Mapping(target = "dependencySoftwareBuildVersionId", expression = "java(new SoftwareBuildVersionId(po.getDependencySoftwareBuildVersionId()))")
    SoftwareBuildVersionDependency toDomain(SoftwareBuildVersionDependencyPo po);
    
    @Mapping(target = "softwareBuildVersionId", expression = "java(domain.getSoftwareBuildVersionId() != null ? domain.getSoftwareBuildVersionId().getValue() : null)")
    @Mapping(target = "dependencySoftwareBuildVersionId", expression = "java(domain.getDependencySoftwareBuildVersionId() != null ? domain.getDependencySoftwareBuildVersionId().getValue() : null)")
    SoftwareBuildVersionDependencyPo toPo(SoftwareBuildVersionDependency domain);
    
    List<SoftwareBuildVersionDependency> toDomainList(List<SoftwareBuildVersionDependencyPo> poList);
    
    List<SoftwareBuildVersionDependencyPo> toPoList(List<SoftwareBuildVersionDependency> domainList);
}
```

- [ ] **Step 4: 创建CompatiblePnConverter**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/CompatiblePnConverter.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CompatiblePn;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 兼件号Domain Model ⇄ Po转换器
 */
@Mapper
public interface CompatiblePnConverter {
    
    CompatiblePnConverter INSTANCE = Mappers.getMapper(CompatiblePnConverter.class);
    
    CompatiblePn toDomain(CompatiblePnPo po);
    
    CompatiblePnPo toPo(CompatiblePn domain);
    
    List<CompatiblePn> toDomainList(List<CompatiblePnPo> poList);
    
    List<CompatiblePnPo> toPoList(List<CompatiblePn> domainList);
}
```

- [ ] **Step 5: 创建VehiclePartConverter**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/VehiclePartConverter.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆零件Domain Model ⇄ Po转换器
 */
@Mapper
public interface VehiclePartConverter {
    
    VehiclePartConverter INSTANCE = Mappers.getMapper(VehiclePartConverter.class);
    
    VehiclePart toDomain(VehiclePartPo po);
    
    VehiclePartPo toPo(VehiclePart domain);
    
    List<VehiclePart> toDomainList(List<VehiclePartPo> poList);
    
    List<VehiclePartPo> toPoList(List<VehiclePart> domainList);
}
```

- [ ] **Step 6: 编译验证**

Run: `cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS (MapStruct会生成实现类)

- [ ] **Step 7: 提交**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/
git commit -m "feat(infra): add domain model converters
- SoftwareBuildVersionConverter
- SoftwarePackageConverter
- SoftwareBuildVersionDependencyConverter
- CompatiblePnConverter
- VehiclePartConverter"
```

---

## Task 8: 创建Repository实现

**Files:**
- Create: `infrastructure/persistence/repository/*.java` (4个文件)

- [ ] **Step 1: 创建SoftwareBuildVersionRepositoryImpl**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/SoftwareBuildVersionRepositoryImpl.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwareBuildVersionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.SoftwareBuildVersionConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionDependencyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 软件内部版本Repository实现
 */
@Repository
@RequiredArgsConstructor
public class SoftwareBuildVersionRepositoryImpl implements SoftwareBuildVersionRepository {
    
    private final SoftwareBuildVersionMapper mapper;
    private final SoftwareBuildVersionConverter converter;
    private final SoftwareBuildVersionPackageMapper packageMapper;
    private final SoftwareBuildVersionDependencyMapper dependencyMapper;
    
    @Override
    public SoftwareBuildVersion findById(SoftwareBuildVersionId id) {
        SoftwareBuildVersionPo po = mapper.selectPoById(id.getValue());
        return converter.toDomain(po);
    }
    
    @Override
    public SoftwareBuildVersion findByDeviceCodeAndPnAndVersion(DeviceCode deviceCode, SoftwarePn pn, String version) {
        SoftwareBuildVersionPo po = mapper.selectPoByDeviceCodeAndSoftwarePnAndSoftwareBuildVer(
            deviceCode.getValue(), pn.getValue(), version
        );
        return converter.toDomain(po);
    }
    
    @Override
    public List<SoftwareBuildVersion> search(Map<String, Object> params) {
        List<SoftwareBuildVersionPo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public List<SoftwareBuildVersion> listByBaselineCode(String baselineCode) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("baselineCode", baselineCode);
        return search(params);
    }
    
    @Override
    public void save(SoftwareBuildVersion version) {
        SoftwareBuildVersionPo po = converter.toPo(version);
        if (version.getId() == null || version.getId().getValue() == null) {
            mapper.insertPo(po);
        } else {
            mapper.updatePo(po);
        }
    }
    
    @Override
    public void deleteByIds(List<SoftwareBuildVersionId> ids) {
        Long[] idArray = ids.stream()
            .map(SoftwareBuildVersionId::getValue)
            .toArray(Long[]::new);
        mapper.batchPhysicalDeletePo(idArray);
    }
    
    @Override
    public int countPackages(SoftwareBuildVersionId id) {
        return packageMapper.countBySoftwareBuildVersionId(id.getValue());
    }
    
    @Override
    public int countDependencies(SoftwareBuildVersionId id) {
        return dependencyMapper.countBySoftwareBuildVersionId(id.getValue());
    }
}
```

- [ ] **Step 2: 创建SoftwarePackageRepositoryImpl**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/SoftwarePackageRepositoryImpl.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwarePackageRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.SoftwarePackageConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 软件包Repository实现
 */
@Repository
@RequiredArgsConstructor
public class SoftwarePackageRepositoryImpl implements SoftwarePackageRepository {
    
    private final SoftwarePackageMapper mapper;
    private final SoftwarePackageConverter converter;
    
    @Override
    public SoftwarePackage findById(SoftwarePackageId id) {
        SoftwarePackagePo po = mapper.selectPoById(id.getValue());
        return converter.toDomain(po);
    }
    
    @Override
    public List<SoftwarePackage> findByIds(List<SoftwarePackageId> ids) {
        List<Long> idValues = ids.stream()
            .map(SoftwarePackageId::getValue)
            .toList();
        List<SoftwarePackagePo> poList = mapper.selectPoByIds(idValues);
        return converter.toDomainList(poList);
    }
    
    @Override
    public List<SoftwarePackage> search(Map<String, Object> params) {
        List<SoftwarePackagePo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public void save(SoftwarePackage package) {
        SoftwarePackagePo po = converter.toPo(package);
        if (package.getId() == null || package.getId().getValue() == null) {
            mapper.insertPo(po);
        } else {
            mapper.updatePo(po);
        }
    }
    
    @Override
    public void deleteByIds(List<SoftwarePackageId> ids) {
        Long[] idArray = ids.stream()
            .map(SoftwarePackageId::getValue)
            .toArray(Long[]::new);
        mapper.batchPhysicalDeletePo(idArray);
    }
}
```

- [ ] **Step 3: 创建CompatiblePnRepositoryImpl**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/CompatiblePnRepositoryImpl.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CompatiblePn;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.CompatiblePnRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.CompatiblePnConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.CompatiblePnMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 兼件号Repository实现
 */
@Repository
@RequiredArgsConstructor
public class CompatiblePnRepositoryImpl implements CompatiblePnRepository {
    
    private final CompatiblePnMapper mapper;
    private final CompatiblePnConverter converter;
    
    @Override
    public CompatiblePn findById(Long id) {
        CompatiblePnPo po = mapper.selectPoById(id);
        return converter.toDomain(po);
    }
    
    @Override
    public List<CompatiblePn> search(Map<String, Object> params) {
        List<CompatiblePnPo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public void save(CompatiblePn compatiblePn) {
        CompatiblePnPo po = converter.toPo(compatiblePn);
        if (compatiblePn.getId() == null) {
            mapper.insertPo(po);
        } else {
            mapper.updatePo(po);
        }
    }
    
    @Override
    public void deleteByIds(List<Long> ids) {
        Long[] idArray = ids.toArray(Long[]::new);
        mapper.batchPhysicalDeletePo(idArray);
    }
}
```

- [ ] **Step 4: 创建VehiclePartRepositoryImpl**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/VehiclePartRepositoryImpl.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.VehiclePartConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车辆零件Repository实现
 */
@Repository
@RequiredArgsConstructor
public class VehiclePartRepositoryImpl implements VehiclePartRepository {
    
    private final VehiclePartMapper mapper;
    private final VehiclePartConverter converter;
    
    @Override
    public VehiclePart findById(Long id) {
        VehiclePartPo po = mapper.selectPoById(id);
        return converter.toDomain(po);
    }
    
    @Override
    public List<VehiclePart> search(Map<String, Object> params) {
        List<VehiclePartPo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public void save(VehiclePart vehiclePart) {
        VehiclePartPo po = converter.toPo(vehiclePart);
        if (vehiclePart.getId() == null) {
            mapper.insertPo(po);
        } else {
            mapper.updatePo(po);
        }
    }
    
    @Override
    public void deleteByIds(List<Long> ids) {
        Long[] idArray = ids.toArray(Long[]::new);
        mapper.batchPhysicalDeletePo(idArray);
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/
git commit -m "feat(infra): add repository implementations
- SoftwareBuildVersionRepositoryImpl
- SoftwarePackageRepositoryImpl
- CompatiblePnRepositoryImpl
- VehiclePartRepositoryImpl"
```

---

## Task 9: 迁移Domain层异常

**Files:**
- Move: `common/exception/PotaBaseException.java` → `domain/exception/PotaBaseException.java`

- [ ] **Step 1: 读取现有异常文件**

Run: 读取 `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/common/exception/PotaBaseException.java`

- [ ] **Step 2: 在domain层创建异常类**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/exception/PotaBaseException.java`

保持现有内容不变，仅移动包路径。

- [ ] **Step 3: 更新现有代码引用**

搜索并替换所有 `import net.hwyz.iov.cloud.iov.ota.service.common.exception.PotaBaseException;`
改为 `import net.hwyz.iov.cloud.iov.ota.service.domain.exception.PotaBaseException;`

- [ ] **Step 4: 删除common层异常文件**

```bash
rm iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/common/exception/PotaBaseException.java
```

- [ ] **Step 5: 编译验证**

Run: `cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/exception/
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/common/exception/
git commit -m "refactor(domain): move PotaBaseException to domain layer"
```

---

## Task 10: 创建Domain Service（空实现）

**Files:**
- Create: `domain/service/*.java` (4个文件，第一阶段空实现)

- [ ] **Step 1: 创建SoftwareBuildVersionDomainService**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/service/SoftwareBuildVersionDomainService.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwareBuildVersionRepository;

/**
 * 软件内部版本领域服务
 * 第一阶段：空实现，后续阶段填充业务规则
 */
@RequiredArgsConstructor
public class SoftwareBuildVersionDomainService {
    
    private final SoftwareBuildVersionRepository repository;
    
}
```

- [ ] **Step 2: 创建SoftwarePackageDomainService**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/service/SoftwarePackageDomainService.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwarePackageRepository;

/**
 * 软件包领域服务
 * 第一阶段：空实现
 */
@RequiredArgsConstructor
public class SoftwarePackageDomainService {
    
    private final SoftwarePackageRepository repository;
    
}
```

- [ ] **Step 3: 创建CompatiblePnDomainService**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/service/CompatiblePnDomainService.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.CompatiblePnRepository;

/**
 * 兼件号领域服务
 * 第一阶段：空实现
 */
@RequiredArgsConstructor
public class CompatiblePnDomainService {
    
    private final CompatiblePnRepository repository;
    
}
```

- [ ] **Step 4: 创建VehiclePartDomainService**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/service/VehiclePartDomainService.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehiclePartRepository;

/**
 * 车辆零件领域服务
 * 第一阶段：空实现
 */
@RequiredArgsConstructor
public class VehiclePartDomainService {
    
    private final VehiclePartRepository repository;
    
}
```

- [ ] **Step 5: 编译验证**

Run: `cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/service/
git commit -m "feat(domain): add domain services (phase1 empty impl)
- SoftwareBuildVersionDomainService
- SoftwarePackageDomainService
- CompatiblePnDomainService
- VehiclePartDomainService"
```

---

## Task 11: 整体编译验证

**Files:**
- 无新增文件，仅验证

- [ ] **Step 1: 清理并重新编译**

```bash
cd iov-ota-service && mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行单元测试（如有）**

```bash
cd iov-ota-service && mvn test
```

Expected: Tests run successfully

- [ ] **Step 3: 提交最终版本**

```bash
git add .
git commit -m "feat(phase1): complete domain layer and repository implementation"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- [x] Domain Model创建（值对象、实体、聚合根）
- [x] Repository接口定义
- [x] Repository实现（委托Mapper）
- [x] Converter创建
- [x] Domain Service创建（空实现）
- [x] Domain层异常迁移

**2. Placeholder scan:**
- [x] 无"TBD"、"TODO"
- [x] 所有代码完整
- [x] 无"类似Task N"

**3. Type consistency:**
- [x] 值对象类型一致
- [x] Repository接口方法签名一致
- [x] Converter映射一致

---

## Phase 1 Complete

第一阶段完成。Domain层已建立，Repository已实现。

**下一步：**
- 第二阶段：引入DTO层，修正Application和Controller违规
- 第三阶段：业务规则迁移到Domain Model

---

## 结束