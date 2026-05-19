# OTA服务DDD架构重构设计文档

## 文档信息
- **日期**: 2026-05-19
- **项目**: iov-cloud-iov-ota
- **类型**: 架构重构
- **方案**: 渐进式三层重构

---

## 问题诊断

### 严重DDD违规（必须修复）

| 问题 | 当前状态 | 规范要求 | 影响 |
|------|---------|---------|------|
| **缺少Domain层** | 无`domain/`包目录 | 必须有domain/model、service、repository | 业务逻辑无核心承载层 |
| **Application直接用PO** | `AppService`返回`SoftwareBuildVersionPo` | Application应返回DTO或Domain Model | 跨层对象泄漏 |
| **Application直接用Mapper** | 直接调用`softwareBuildVersionMapper` | 应通过Repository接口 | Infrastructure层穿透 |
| **Controller直接用PO** | `MptController`操作`Po`对象 | Controller只应接触VO | 三层穿透违规 |

### 中等问题

| 问题 | 当前状态 | 规范要求 |
|------|---------|---------|
| **跨层转换违规** | Controller: `PO → VO` | Controller: `VO ⇄ DTO` |
| **业务规则位置** | Application层的`checkUnique` | Domain Model/Domain Service |
| **VO含审计字段** | `SoftwareBuildVersionMpt`含`createTime` | VO不应含数据库审计字段 |

### 结构问题

- **缺少DTO定义**：Application层无DTO包
- **缺少Repository接口**：Domain层应定义Repository接口
- **缺少Domain Model**：无Entity、ValueObject定义

---

## 重构方案：渐进式三层重构

### 约束条件

- ✅ 无约束（可以重新设计）
- ✅ 必须保证编译通过和测试通过
- ✅ 渐进式重构（可分阶段进行）
- ✅ 保持API兼容性（Feign接口不变）

---

## 第一阶段：创建Domain层和Repository接口

### 目标

建立DDD核心架构，不破坏现有功能，作为后续重构的基础。

### 新增目录结构

```
iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/
│
├── domain/                              【新增 - 领域层】
│   ├── model/
│   │   ├── aggregate/                   // 聚合根
│   │   │   └── SoftwareBuildVersion.java
│   │   ├── entity/                      // 实体
│   │   │   ├── SoftwarePackage.java
│   │   │   ├── SoftwareBuildVersionDependency.java
│   │   │   ├── CompatiblePn.java
│   │   │   └── VehiclePart.java
│   │   ├── valueobject/                 // 值对象
│   │   │   ├── SoftwareBuildVersionId.java
│   │   │   ├── DeviceCode.java
│   │   │   └── SoftwarePn.java
│   │   └── event/                       // 领域事件（预留）
│   ├── service/                         // 领域服务
│   │   ├── SoftwareBuildVersionDomainService.java
│   │   ├── SoftwarePackageDomainService.java
│   │   ├── CompatiblePnDomainService.java
│   │   └── VehiclePartDomainService.java
│   ├── repository/                      // Repository接口定义
│   │   ├── SoftwareBuildVersionRepository.java
│   │   ├── SoftwarePackageRepository.java
│   │   ├── CompatiblePnRepository.java
│   │   └── VehiclePartRepository.java
│   ├── gateway/                         // （可选）领域级外部依赖接口
│   ├── policy/                          // 业务策略（预留）
│   ├── factory/                         // 聚合工厂
│   │   └── SoftwareBuildVersionFactory.java
│   └── exception/                       // 领域异常
│       └── SoftwareBuildVersionNotExistException.java（迁移现有）
│
├── infrastructure/
│   ├── persistence/
│   │   ├── repository/                  【新增 - Repository实现】
│   │   │   ├── SoftwareBuildVersionRepositoryImpl.java
│   │   │   ├── SoftwarePackageRepositoryImpl.java
│   │   │   ├── CompatiblePnRepositoryImpl.java
│   │   │   └── VehiclePartRepositoryImpl.java
│   │   └── converter/                   // Domain Model ⇄ Po 转换
│   │       ├── SoftwareBuildVersionConverter.java（新增）
│   │       ├── SoftwarePackageConverter.java（新增）
│   │       ├── CompatiblePnConverter.java（新增）
│   │       └── VehiclePartPoConverter.java（保留现有）
```

### Domain Model设计

#### 聚合根：SoftwareBuildVersion

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

@Data
@Builder
public class SoftwareBuildVersion {
    private SoftwareBuildVersionId id;
    private DeviceCode deviceCode;
    private SoftwarePn softwarePn;
    private String softwareBuildVer;
    private SoftwareVersionStatus status;
    private Instant releaseDate;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private List<SoftwarePackage> packages;
    private List<SoftwareBuildVersionDependency> dependencies;
}
```

#### 实体：SoftwarePackage

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

@Data
@Builder
public class SoftwarePackage {
    private SoftwarePackageId id;
    private DeviceCode deviceCode;
    private SoftwarePn softwarePn;
    private String packageCode;
    private String packageName;
    private SoftwarePackageType packageType;
    private String packageUrl;
    private String packageMd5;
    private Long packageSize;
}
```

#### 值对象：SoftwareBuildVersionId

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoftwareBuildVersionId {
    private Long value;
}
```

### Repository接口设计

```java
package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

public interface SoftwareBuildVersionRepository {
    SoftwareBuildVersion findById(SoftwareBuildVersionId id);
    SoftwareBuildVersion findByDeviceCodeAndPnAndVersion(DeviceCode deviceCode, SoftwarePn pn, String version);
    List<SoftwareBuildVersion> search(SoftwareBuildVersionQuery query);
    void save(SoftwareBuildVersion version);
    void deleteById(SoftwareBuildVersionId id);
    int countPackages(SoftwareBuildVersionId id);
    int countDependencies(SoftwareBuildVersionId id);
}
```

### Repository实现设计

```java
package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

@Repository
@RequiredArgsConstructor
public class SoftwareBuildVersionRepositoryImpl implements SoftwareBuildVersionRepository {
    
    private final SoftwareBuildVersionMapper mapper;
    private final SoftwareBuildVersionConverter converter;
    
    @Override
    public SoftwareBuildVersion findById(SoftwareBuildVersionId id) {
        SoftwareBuildVersionPo po = mapper.selectPoById(id.getValue());
        return converter.toDomain(po);
    }
    
    // 其他方法委托给Mapper...
}
```

### 迁移策略

1. **保留现有AppService**：第一阶段不修改Application层
2. **Domain Model暂不承载业务**：先创建结构，业务规则迁移在第三阶段
3. **Repository作为Mapper的代理**：初期实现直接委托给现有Mapper

---

## 第二阶段：引入DTO层，修正Application和Controller违规

### 目标

修复Application层直接使用PO、Controller层直接使用PO的违规问题，建立正确的对象转换链。

### 新增目录结构

```
iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/
│
├── application/                         【扩展 - 应用层】
│   ├── service/                         // AppService（修改）
│   │   ├── SoftwareBuildVersionAppService.java
│   │   ├── SoftwarePackageAppService.java
│   │   ├── CompatiblePnAppService.java
│   │   ├── VehiclePartAppService.java
│   │   └── DataSyncRecordAppService.java
│   ├── dto/                             【新增 - DTO定义】
│   │   ├── cmd/                         // 写入类入参
│   │   │   ├── CreateSoftwareBuildVersionCmd.java
│   │   │   ├── ModifySoftwareBuildVersionCmd.java
│   │   │   ├── AddPackageCmd.java
│   │   │   └── AddDependencyCmd.java
│   │   ├── query/                       // 查询类入参
│   │   │   ├── SoftwareBuildVersionQuery.java
│   │   │   ├── SoftwarePackageQuery.java
│   │   │   ├── CompatiblePnQuery.java
│   │   │   └── VehiclePartQuery.java
│   │   └── result/                      // 出参
│   │       ├── SoftwareBuildVersionDto.java
│   │       ├── SoftwarePackageDto.java
│   │       ├── SoftwareBuildVersionDetailDto.java
│   │       ├── CompatiblePnDto.java
│   │       └── VehiclePartDto.java
│   ├── assembler/                       【新增 - DTO ⇄ Domain Model转换】
│   │   ├── SoftwareBuildVersionAssembler.java
│   │   ├── SoftwarePackageAssembler.java
│   │   ├── CompatiblePnAssembler.java
│   │   └── VehiclePartAssembler.java
│   └── port/                            【新增 - Application定义的出站端口】
│       ├── gateway/                     // 外部系统Port（预留）
│       └── service/                     // 技术能力Port（预留）
│
├── adapter/web/
│   ├── controller/
│   │   ├── mpt/
│   │   │   ├── MptSoftwareBuildVersionController.java（修改）
│   │   │   ├── MptSoftwarePackageController.java（修改）
│   │   │   ├── MptCompatiblePnController.java（修改）
│   │   ├── vo/                          【新增 - Controller层VO】
│   │   │   ├── request/                 // 入参VO
│   │   │   │   ├── SoftwareBuildVersionMptRequest.java
│   │   │   │   ├── SoftwarePackageMptRequest.java
│   │   │   │   ├── CompatiblePnMptRequest.java
│   │   │   └── response/                // 出参VO
│   │   │       ├── SoftwareBuildVersionMptResponse.java
│   │   │       ├── SoftwarePackageMptResponse.java
│   │   │       ├── CompatiblePnMptResponse.java
│   │   └── assembler/                   【修改 - VO ⇄ DTO转换】
│   │       ├── SoftwareBuildVersionMptAssembler.java
│   │       ├── SoftwarePackageMptAssembler.java
│   │       ├── CompatiblePnMptAssembler.java
```

### 对象转换链

```
Controller(VO) ←→ Assembler ←→ Application(DTO) ←→ Assembler ←→ Domain(Model) ←→ Converter ←→ Infrastructure(PO) ←→ Mapper
```

### DTO设计

#### 查询结果DTO

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

@Data
@Builder
public class SoftwareBuildVersionDto {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String softwareSource;
    private Instant releaseDate;
    private Integer softwarePackageCount;
    private Integer dependencyCount;
}
```

#### 创建命令DTO

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

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
}
```

#### 查询条件DTO

```java
package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

@Data
public class SoftwareBuildVersionQuery {
    private String deviceCode;
    private String softwarePn;
    private String baselineCode;
    private Instant beginTime;
    private Instant endTime;
}
```

### Controller VO设计

#### 请求VO

```java
package net.hwyz.iov.cloud.iov.ota.service.adapter.web.vo.request;

@Data
public class SoftwareBuildVersionMptRequest extends BaseRequest {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    @NotBlank(message = "软件说明不能为空")
    private String softwareDesc;
}
```

#### 响应VO

```java
package net.hwyz.iov.cloud.iov.ota.service.adapter.web.vo.response;

@Data
@Builder
@JsonPropertyOrder({"id", "device_code", "software_pn"})
public class SoftwareBuildVersionMptResponse {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("device_code")
    private String deviceCode;
    
    @JsonProperty("software_pn")
    private String softwarePn;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("release_date")
    private Instant releaseDate;
}
```

### Assembler修正

**修正前（违规）**：
```java
SoftwareBuildVersionMpt fromPo(SoftwareBuildVersionPo po);
SoftwareBuildVersionPo toPo(SoftwareBuildVersionMpt vo);
```

**修正后（合规）**：
```java
// Controller层：VO ⇄ DTO
SoftwareBuildVersionMptResponse fromDto(SoftwareBuildVersionDto dto);
CreateSoftwareBuildVersionCmd toCmd(SoftwareBuildVersionMptRequest request);

// Application层：DTO ⇄ Domain Model
SoftwareBuildVersionDto toDto(SoftwareBuildVersion domain);
SoftwareBuildVersion toDomain(CreateSoftwareBuildVersionCmd cmd);
```

### AppService修正

**修正前（违规）**：
```java
public List<SoftwareBuildVersionPo> search(String key) {
    return softwareBuildVersionMapper.selectPoByMap(map);
}
```

**修正后（合规）**：
```java
public List<SoftwareBuildVersionDto> search(SoftwareBuildVersionQuery query) {
    List<SoftwareBuildVersion> domainList = repository.search(query);
    return SoftwareBuildVersionAssembler.toDtoList(domainList);
}
```

### Controller修正

**修正前（违规）**：
```java
@GetMapping("/list")
public ApiResponse<PageResult<SoftwareBuildVersionMpt>> list(SoftwareBuildVersionMpt vo) {
    startPage();
    List<SoftwareBuildVersionPo> poList = appService.search(...);
    List<SoftwareBuildVersionMpt> voList = PageUtil.convert(poList, Assembler::fromPo);
    return ApiResponse.ok(getPageResult(voList));
}
```

**修正后（合规）**：
```java
@GetMapping("/list")
public ApiResponse<PageResult<SoftwareBuildVersionMptResponse>> list(SoftwareBuildVersionMptRequest request) {
    startPage();
    SoftwareBuildVersionQuery query = MptAssembler.toQuery(request);
    List<SoftwareBuildVersionDto> dtoList = appService.search(query);
    List<SoftwareBuildVersionMptResponse> responseList = PageUtil.convert(dtoList, MptAssembler::fromDto);
    return ApiResponse.ok(getPageResult(responseList));
}
```

---

## 第三阶段：业务规则迁移到Domain Model

### 目标

将散落在Application层的业务规则迁移到Domain Model和Domain Service，实现充血模型。

### 业务规则识别

| 当前位置 | 业务规则 | 应迁移到 |
|---------|---------|---------|
| `SoftwareBuildVersionAppService.checkDeviceCodeAndSoftwarePnUnique()` | 唯一性检查 | `SoftwareBuildVersion` Domain Model |
| `SoftwareBuildVersionAppService.createPackage()` | 包关联创建逻辑 | `SoftwareBuildVersion.addPackage()` |
| `SoftwareBuildVersionAppService.createDependency()` | 依赖创建逻辑 | `SoftwareBuildVersion.addDependency()` |
| `SoftwareBuildVersionAppService.modifyDependency()` | 依赖修改逻辑 | `SoftwareBuildVersionDependency.setAdaptiveLevel()` |

### Domain Model充血设计

#### SoftwareBuildVersion（聚合根）

```java
@Data
public class SoftwareBuildVersion {
    private SoftwareBuildVersionId id;
    private DeviceCode deviceCode;
    private SoftwarePn softwarePn;
    private String softwareBuildVer;
    private SoftwareVersionStatus status;
    private Instant releaseDate;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private List<SoftwarePackage> packages = new ArrayList<>();
    private List<SoftwareBuildVersionDependency> dependencies = new ArrayList<>();

    /**
     * 检查唯一性
     */
    public boolean isDuplicateWith(SoftwareBuildVersion other) {
        if (other == null) return false;
        return this.deviceCode.equals(other.deviceCode)
            && this.softwarePn.equals(other.softwarePn)
            && this.softwareBuildVer.equals(other.softwareBuildVer)
            && !this.id.equals(other.id);
    }

    /**
     * 添加软件包（业务规则：不能重复）
     */
    public void addPackage(SoftwarePackage package) {
        if (packages.stream().anyMatch(p -> p.getId().equals(package.getId()))) {
            throw new BusinessException("软件包已存在，不能重复添加");
        }
        packages.add(package);
    }

    /**
     * 批量添加软件包
     */
    public void addPackages(List<SoftwarePackage> newPackages) {
        newPackages.forEach(this::addPackage);
    }

    /**
     * 添加依赖（业务规则：不能重复）
     */
    public void addDependency(SoftwareBuildVersionDependency dependency) {
        if (dependencies.stream().anyMatch(d -> 
            d.getDependencySoftwareBuildVersionId().equals(dependency.getDependencySoftwareBuildVersionId()))) {
            throw new BusinessException("依赖关系已存在");
        }
        dependencies.add(dependency);
    }

    /**
     * 修改依赖适配级别
     */
    public void modifyDependencyAdaptiveLevel(SoftwareBuildVersionId dependencyId, Integer adaptiveLevel) {
        dependencies.stream()
            .filter(d -> d.getDependencySoftwareBuildVersionId().equals(dependencyId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("依赖关系不存在"))
            .setAdaptiveLevel(adaptiveLevel);
    }
}
```

#### SoftwareBuildVersionDependency（实体）

```java
@Data
public class SoftwareBuildVersionDependency {
    private SoftwareBuildVersionDependencyId id;
    private SoftwareBuildVersionId softwareBuildVersionId;
    private SoftwareBuildVersionId dependencySoftwareBuildVersionId;
    private Integer adaptiveLevel;
    private Integer sort;

    /**
     * 设置适配级别（业务规则：0-100）
     */
    public void setAdaptiveLevel(Integer level) {
        if (level == null || level < 0 || level > 100) {
            throw new BusinessException("适配级别必须在0-100之间");
        }
        this.adaptiveLevel = level;
    }
}
```

### Domain Service设计

注意：Domain Service是纯Java类，不使用Spring注解，由Application层注入和管理。

```java
@RequiredArgsConstructor
public class SoftwareBuildVersionDomainService {

    private final SoftwareBuildVersionRepository repository;

    /**
     * 检查唯一性（跨聚合）
     */
    public boolean checkUnique(DeviceCode deviceCode, SoftwarePn softwarePn, 
                               String softwareBuildVer, SoftwareBuildVersionId excludeId) {
        SoftwareBuildVersion existing = repository.findByDeviceCodeAndPnAndVersion(
            deviceCode, softwarePn, softwareBuildVer
        );
        if (existing == null) return true;
        return existing.getId().equals(excludeId);
    }

    /**
     * 创建软件版本
     */
    public SoftwareBuildVersion create(CreateSoftwareBuildVersionCmd cmd) {
        if (!checkUnique(cmd.getDeviceCode(), cmd.getSoftwarePn(), 
                        cmd.getSoftwareBuildVer(), null)) {
            throw new BusinessException("软件版本已存在");
        }
        
        return SoftwareBuildVersion.builder()
            .deviceCode(cmd.getDeviceCode())
            .softwarePn(cmd.getSoftwarePn())
            .softwareBuildVer(cmd.getSoftwareBuildVer())
            .status(SoftwareVersionStatus.ACTIVE)
            .build();
    }
}
```

### Application Service重构

```java
@Service
@RequiredArgsConstructor
public class SoftwareBuildVersionAppService {

    private final SoftwareBuildVersionRepository repository;
    private final SoftwareBuildVersionDomainService domainService;

    /**
     * 创建（业务规则在Domain Service）
     */
    public Long create(CreateSoftwareBuildVersionCmd cmd) {
        SoftwareBuildVersion domain = domainService.create(cmd);
        repository.save(domain);
        return domain.getId().getValue();
    }

    /**
     * 添加软件包（业务规则在Domain Model）
     */
    public void addPackage(SoftwareBuildVersionId versionId, List<SoftwarePackageId> packageIds) {
        SoftwareBuildVersion version = repository.findById(versionId);
        List<SoftwarePackage> packages = packageDomainService.findByIds(packageIds);
        version.addPackages(packages);
        repository.save(version);
    }
}
```

---

## 验证检查清单

### 分层架构验证

```
[ ] Domain层未引入Spring、MyBatis等框架依赖
[ ] Domain Model字段无ORM注解（@TableName、@TableField等）
[ ] Repository接口位于domain/repository，实现在infrastructure
[ ] 业务规则位于Domain Model或Domain Service
[ ] Application Service仅做编排，无业务规则
```

### 对象转换验证

```
[ ] Controller方法签名无PO、Domain Model类型
[ ] Application Service方法签名无PO、VO类型
[ ] 所有跨层数据传递有显式转换器
[ ] 转换器归属层正确
[ ] 使用MapStruct进行对象转换
```

### API兼容性验证

```
[ ] Feign接口（OtaXxxService）保持不变
[ ] API层VO对象保持字段兼容
[ ] HTTP接口路径保持不变
```

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 大量新增代码 | 编译错误 | 分阶段实施，每阶段验证编译 |
| 对象转换遗漏 | 数据丢失 | 使用MapStruct，编译期检查 |
| Repository实现委托Mapper | 性能影响 | 初期委托，后续优化SQL |
| 业务规则迁移遗漏 | 功能异常 | 第三阶段仔细识别每个业务规则 |

---

## 附录：现有文件清单

### 需要修改的文件

1. Application Service（5个）
   - `SoftwareBuildVersionAppService.java`
   - `SoftwarePackageAppService.java`
   - `CompatiblePnAppService.java`
   - `VehiclePartAppService.java`
   - `DataSyncRecordAppService.java`

2. Controller（7个）
   - `MptSoftwareBuildVersionController.java`
   - `MptSoftwarePackageController.java`
   - `MptCompatiblePnController.java`
   - `CompatiblePnServiceController.java`
   - `SoftwareBuildVersionServiceController.java`
   - `VehiclePartServiceController.java`
   - `OpenBomController.java`

3. Assembler（现有迁移）
   - `SoftwareBuildVersionMptAssembler.java`
   - `SoftwarePackageMptAssembler.java`
   - `CompatiblePnMptAssembler.java`

### 需要新增的文件（预估）

- Domain Model：约15个
- Repository接口：约5个
- Repository实现：约5个
- DTO：约20个
- Application Assembler：约5个
- Controller VO：约15个
- Domain Service：约5个
- Factory：约5个

---

## 结束

设计文档完成。等待用户审查后进入实现阶段。