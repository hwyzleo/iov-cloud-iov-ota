# OTA服务DDD架构重构 - 第二阶段实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 引入DTO层，修正Application层直接使用PO、Controller层直接使用PO的违规问题，建立正确的对象转换链。

**Architecture:** 
- Application层新增dto包（cmd、query、result）
- Application层新增assembler包（DTO ⇄ Domain Model转换）
- Controller层Assembler改为VO ⇄ DTO转换
- Application Service改为返回DTO，使用Repository替代Mapper

**Tech Stack:** 
- Java 17
- Spring Boot
- MapStruct（Assembler）
- Lombok

---

## File Structure

### 新增文件

**Application层DTO（约20个文件）**

| 文件路径 | 职责 |
|---------|------|
| `application/dto/cmd/CreateSoftwareBuildVersionCmd.java` | 创建软件版本命令 |
| `application/dto/cmd/ModifySoftwareBuildVersionCmd.java` | 修改软件版本命令 |
| `application/dto/cmd/AddPackageCmd.java` | 添加软件包命令 |
| `application/dto/cmd/AddDependencyCmd.java` | 添加依赖命令 |
| `application/dto/cmd/CreateSoftwarePackageCmd.java` | 创建软件包命令 |
| `application/dto/cmd/CreateCompatiblePnCmd.java` | 创建兼件号命令 |
| `application/dto/cmd/CreateVehiclePartCmd.java` | 创建车辆零件命令 |
| `application/dto/query/SoftwareBuildVersionQuery.java` | 软件版本查询条件 |
| `application/dto/query/SoftwarePackageQuery.java` | 软件包查询条件 |
| `application/dto/query/CompatiblePnQuery.java` | 兼件号查询条件 |
| `application/dto/query/VehiclePartQuery.java` | 车辆零件查询条件 |
| `application/dto/result/SoftwareBuildVersionDto.java` | 软件版本结果 |
| `application/dto/result/SoftwarePackageDto.java` | 软件包结果 |
| `application/dto/result/SoftwareBuildVersionDetailDto.java` | 软件版本详情 |
| `application/dto/result/CompatiblePnDto.java` | 兼件号结果 |
| `application/dto/result/VehiclePartDto.java` | 车辆零件结果 |

**Application层Assembler（约5个文件）**

| 文件路径 | 职责 |
|---------|------|
| `application/assembler/SoftwareBuildVersionAssembler.java` | DTO ⇄ Domain Model |
| `application/assembler/SoftwarePackageAssembler.java` | DTO ⇄ Domain Model |
| `application/assembler/CompatiblePnAssembler.java` | DTO ⇄ Domain Model |
| `application/assembler/VehiclePartAssembler.java` | DTO ⇄ Domain Model |
| `application/assembler/SoftwareBuildVersionDependencyAssembler.java` | DTO ⇄ Domain Model |

### 修改文件

**Application Service（5个文件）**
- `SoftwareBuildVersionAppService.java` - 改为使用Repository和DTO
- `SoftwarePackageAppService.java` - 改为使用Repository和DTO
- `CompatiblePnAppService.java` - 改为使用Repository和DTO
- `VehiclePartAppService.java` - 改为使用Repository和DTO
- `DataSyncRecordAppService.java` - 改为使用Repository和DTO

**Controller层Assembler（现有迁移）**
- `SoftwareBuildVersionMptAssembler.java` - 改为VO ⇄ DTO
- `SoftwarePackageMptAssembler.java` - 改为VO ⇄ DTO
- `CompatiblePnMptAssembler.java` - 改为VO ⇄ DTO

**Controller（7个文件）**
- `MptSoftwareBuildVersionController.java` - 改为使用DTO
- `MptSoftwarePackageController.java` - 改为使用DTO
- `MptCompatiblePnController.java` - 改为使用DTO
- `SoftwareBuildVersionServiceController.java` - 改为使用DTO
- `CompatiblePnServiceController.java` - 改为使用DTO
- `VehiclePartServiceController.java` - 改为使用DTO
- `OpenBomController.java` - 改为使用DTO

---

## Task 1: 创建DTO目录结构

**Files:**
- Create: `application/dto/cmd/` (目录)
- Create: `application/dto/query/` (目录)
- Create: `application/dto/result/` (目录)
- Create: `application/assembler/` (目录)

- [ ] **Step 1: 创建目录**

```bash
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/cmd
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/query
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/result
mkdir -p iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/assembler
```

---

## Task 2: 创建查询条件DTO（Query）

**Files:**
- Create: `application/dto/query/*.java` (4个文件)

- [ ] **Step 1: 创建SoftwareBuildVersionQuery**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/query/SoftwareBuildVersionQuery.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;

import java.time.Instant;

/**
 * 软件内部版本查询条件
 */
@Data
public class SoftwareBuildVersionQuery {
    private String deviceCode;
    private String softwarePn;
    private String baselineCode;
    private Instant beginTime;
    private Instant endTime;
}
```

- [ ] **Step 2: 创建SoftwarePackageQuery**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/query/SoftwarePackageQuery.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;

/**
 * 软件包查询条件
 */
@Data
public class SoftwarePackageQuery {
    private String deviceCode;
    private String softwarePn;
    private String packageCode;
    private String packageName;
    private Long softwareBuildVersionId;
}
```

- [ ] **Step 3: 创建CompatiblePnQuery**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/query/CompatiblePnQuery.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;

/**
 * 兼件号查询条件
 */
@Data
public class CompatiblePnQuery {
    private String partCode;
    private String compatiblePn;
    private String partName;
    private Integer status;
}
```

- [ ] **Step 4: 创建VehiclePartQuery**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/query/VehiclePartQuery.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;

/**
 * 车辆零件查询条件
 */
@Data
public class VehiclePartQuery {
    private String vehicleModelCode;
    private String partCode;
    private String partPn;
    private String partName;
    private Integer status;
}
```

- [ ] **Step 5: 编译验证**

Run: `cd iov-ota-service && mvn compile -DskipTests`

Expected: BUILD SUCCESS

---

## Task 3: 创建结果DTO（Result）

**Files:**
- Create: `application/dto/result/*.java` (5个文件)

- [ ] **Step 1: 创建SoftwareBuildVersionDto**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/result/SoftwareBuildVersionDto.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 软件内部版本结果DTO
 */
@Data
@Builder
public class SoftwareBuildVersionDto {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private String adaptiveAssemblyPn;
    private String adaptiveSoftwarePn;
    private Instant releaseDate;
    private Integer softwarePackageCount;
    private Integer dependencyCount;
    private Integer adaptiveLevel;
}
```

- [ ] **Step 2: 创建SoftwarePackageDto**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/result/SoftwarePackageDto.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

/**
 * 软件包结果DTO
 */
@Data
@Builder
public class SoftwarePackageDto {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String packageCode;
    private String packageName;
    private String packageType;
    private String packageUrl;
    private String packageMd5;
    private Long packageSize;
    private String packageDesc;
}
```

- [ ] **Step 3: 创建SoftwareBuildVersionDetailDto**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/result/SoftwareBuildVersionDetailDto.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 软件内部版本详情DTO
 */
@Data
@Builder
public class SoftwareBuildVersionDetailDto {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private String adaptiveAssemblyPn;
    private String adaptiveSoftwarePn;
    private Instant releaseDate;
    private List<SoftwarePackageDto> packages;
    private List<SoftwareBuildVersionDependencyDto> dependencies;
}
```

- [ ] **Step 4: 创建SoftwareBuildVersionDependencyDto**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/result/SoftwareBuildVersionDependencyDto.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

/**
 * 软件内部版本依赖结果DTO
 */
@Data
@Builder
public class SoftwareBuildVersionDependencyDto {
    private Long id;
    private Long dependencySoftwareBuildVersionId;
    private String dependencyDeviceCode;
    private String dependencySoftwarePn;
    private String dependencySoftwareBuildVer;
    private Integer adaptiveLevel;
}
```

- [ ] **Step 5: 创建CompatiblePnDto**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/result/CompatiblePnDto.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 兼件号结果DTO
 */
@Data
@Builder
public class CompatiblePnDto {
    private Long id;
    private String partCode;
    private String compatiblePn;
    private String partName;
    private String description;
    private Integer status;
    private Instant createTime;
}
```

- [ ] **Step 6: 创建VehiclePartDto**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/result/VehiclePartDto.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 车辆零件结果DTO
 */
@Data
@Builder
public class VehiclePartDto {
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

- [ ] **Step 7: 编译验证**

---

## Task 4: 创建命令DTO（Cmd）

**Files:**
- Create: `application/dto/cmd/*.java` (7个文件)

- [ ] **Step 1: 创建CreateSoftwareBuildVersionCmd**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/cmd/CreateSoftwareBuildVersionCmd.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;

import java.time.Instant;

/**
 * 创建软件内部版本命令
 */
@Data
public class CreateSoftwareBuildVersionCmd {
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private String adaptiveAssemblyPn;
    private String adaptiveSoftwarePn;
    private Instant releaseDate;
    private String createBy;
}
```

- [ ] **Step 2: 创建ModifySoftwareBuildVersionCmd**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/dto/cmd/ModifySoftwareBuildVersionCmd.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;

import java.time.Instant;

/**
 * 修改软件内部版本命令
 */
@Data
public class ModifySoftwareBuildVersionCmd {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private String adaptiveAssemblyPn;
    private String adaptiveSoftwarePn;
    private Instant releaseDate;
    private String modifyBy;
}
```

- [ ] **Step 3-7: 创建其他命令DTO**

类似模式创建：
- `AddPackageCmd.java`
- `AddDependencyCmd.java`
- `ModifyDependencyCmd.java`
- `CreateCompatiblePnCmd.java`
- `CreateVehiclePartCmd.java`

---

## Task 5: 创建Application层Assembler

**Files:**
- Create: `application/assembler/*.java` (5个文件)

- [ ] **Step 1: 创建SoftwareBuildVersionAssembler**

文件: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/assembler/SoftwareBuildVersionAssembler.java`

```java
package net.hwyz.iov.cloud.iov.ota.service.application.assembler;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.CreateSoftwareBuildVersionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ModifySoftwareBuildVersionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.SoftwareBuildVersionDto;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件内部版本Application层Assembler
 */
@Mapper
public interface SoftwareBuildVersionAssembler {
    
    SoftwareBuildVersionAssembler INSTANCE = Mappers.getMapper(SoftwareBuildVersionAssembler.class);
    
    @Mapping(target = "id", expression = "java(cmd.getId() != null ? new SoftwareBuildVersionId(cmd.getId()) : null)")
    @Mapping(target = "deviceCode", expression = "java(new DeviceCode(cmd.getDeviceCode()))")
    @Mapping(target = "softwarePn", expression = "java(new SoftwarePn(cmd.getSoftwarePn()))")
    SoftwareBuildVersion toDomain(CreateSoftwareBuildVersionCmd cmd);
    
    @Mapping(target = "id", expression = "java(cmd.getId() != null ? new SoftwareBuildVersionId(cmd.getId()) : null)")
    @Mapping(target = "deviceCode", expression = "java(new DeviceCode(cmd.getDeviceCode()))")
    @Mapping(target = "softwarePn", expression = "java(new SoftwarePn(cmd.getSoftwarePn()))")
    SoftwareBuildVersion toDomain(ModifySoftwareBuildVersionCmd cmd);
    
    @Mapping(target = "id", expression = "java(domain.getId() != null ? domain.getId().getValue() : null)")
    @Mapping(target = "deviceCode", expression = "java(domain.getDeviceCode() != null ? domain.getDeviceCode().getValue() : null)")
    @Mapping(target = "softwarePn", expression = "java(domain.getSoftwarePn() != null ? domain.getSoftwarePn().getValue() : null)")
    SoftwareBuildVersionDto toDto(SoftwareBuildVersion domain);
    
    List<SoftwareBuildVersionDto> toDtoList(List<SoftwareBuildVersion> domainList);
}
```

- [ ] **Step 2-5: 创建其他Assembler**

类似模式创建：
- `SoftwarePackageAssembler.java`
- `CompatiblePnAssembler.java`
- `VehiclePartAssembler.java`
- `SoftwareBuildVersionDependencyAssembler.java`

---

## Task 6: 修改Controller层Assembler

**Files:**
- Modify: `adapter/web/assembler/*.java` (现有3个文件)

修改策略：
- 改为VO ⇄ DTO转换
- 删除fromPo/toPo方法
- 添加fromDto/toCmd方法

- [ ] **Step 1: 修改SoftwareBuildVersionMptAssembler**

修改 `SoftwareBuildVersionMptAssembler.java`：
- 删除 `fromPo(SoftwareBuildVersionPo)` 方法
- 删除 `toPo(SoftwareBuildVersionMpt)` 方法
- 添加 `fromDto(SoftwareBuildVersionDto)` 方法
- 添加 `toCmd(SoftwareBuildVersionMpt)` 方法
- 添加 `toQuery(SoftwareBuildVersionMpt)` 方法

---

## Task 7: 修改Application Service

**Files:**
- Modify: `application/service/*.java` (5个文件)

修改策略：
- 改为使用Repository替代Mapper
- 改为返回DTO替代PO
- 改为接受Cmd/Query替代散装参数

- [ ] **Step 1: 修改SoftwareBuildVersionAppService**

主要修改：
1. 注入Repository替代Mapper
2. search方法改为返回DTO
3. create方法改为接受Cmd
4. modify方法改为接受Cmd

---

## Task 8: 修改Controller

**Files:**
- Modify: `adapter/web/controller/**/*.java` (7个文件)

修改策略：
- 改为使用DTO
- 修改Assembler调用

---

## Task 9: 编译验证

- [ ] **Step 1: 清理编译**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 提交**

---

## 结束

第二阶段完成后，Application层和Controller层不再直接使用PO对象。