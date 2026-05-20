# FOTA 模块合并到 iov-ota 设计文档

## 文档信息
- **日期**: 2026-05-20
- **项目**: iov-cloud-iov-ota
- **类型**: 跨项目模块合并
- **方案**: 一次性 DDD 结构映射合并

---

## 背景

`iov-cloud-ota-fota`（源）包含 OTA 系统的 Activity（活动管理）、Task（任务管理）、TaskVehicle（任务车辆进度）、Vehicle（车辆管理）、Article（文章管理）五大业务模块。
`iov-cloud-iov-ota`（目标）已按 DDD 架构重构，包含 SoftwareBuildVersion、SoftwarePackage、CompatiblePn、VehiclePart 四个领域。

目标：将 fota 全部代码（fota-api + fota-service）按目标项目的 DDD 分层结构映射合并，包名从 `net.hwyz.iov.cloud.ota.fota` 改为 `net.hwyz.iov.cloud.iov.ota`。

---

## 源项目结构分析

### fota-api（30个文件）
- `api/contract/` — 17个 VO/DTO 接口（ActivityMpt, TaskMpt, VehicleMpt 等 Mpt/CCP 端 VO）
- `api/contract/enums/` — 8个枚举
- `api/feign/ccp/` — FotaCcpApi（CCP端 Feign 接口）
- `api/feign/mpt/` — 5个 MptApi（**不迁移**）

### fota-service（95个文件）
- `service/facade/mpt/` — 5个 Controller（Activity/Task/TaskVehicle/Vehicle/Article）
- `service/facade/ccp/` — FotaCcpController
- `service/facade/assembler/` — 15个 VO 转换器
- `service/application/service/` — 5个 AppService
- `service/domain/activity/` — 6个 model + 1个 repository
- `service/domain/task/` — 3个 model + 1个 repository + 2个 service
- `service/domain/taskvehicle/` — 3个 model + 1个 repository
- `service/domain/vehicle/` — 2个 model + 1个 repository + 1个 factory
- `service/infrastructure/repository/` — 4个 impl + 14个 dao + 14个 po + 5个 assembler
- `service/infrastructure/cache/` — CacheService + 实现
- `service/infrastructure/exception/` — 4个异常
- `service/infrastructure/util/` — FotaHelper

---

## 映射规则

### 一、fota-api → iov-ota-api

| 源路径 | 目标路径 | 说明 |
|---|---|---|
| `api/contract/ActivityMpt` 等 | `api/vo/` | 管理端 VO，直接复制 |
| `api/contract/CloudFotaInfoCcp` 等 | `api/vo/` | CCP 端 VO，直接复制 |
| `api/contract/enums/*` | `api/vo/enums/` | 枚举 |
| `api/feign/ccp/FotaCcpApi` | `api/service/FotaCcpService` | 保留 Feign 接口 |
| `api/feign/mpt/*` | — | **不迁移** |

### 二、fota-service → iov-ota-service

| 源路径 | 目标路径 | 说明 |
|---|---|---|
| `service/facade/mpt/*Controller` | `service/adapter/web/controller/mpt/` | 去掉 MptApi 接口实现 |
| `service/facade/ccp/FotaCcpController` | `service/adapter/web/controller/ccp/FotaCcpController` | 新建 ccp 包 |
| `service/facade/assembler/*` | `service/adapter/web/assembler/` | |
| `service/application/service/*AppService` | `service/application/service/` | |
| `service/domain/{activity,task,taskvehicle,vehicle}/model/*` | `service/domain/model/entity/` | 合并到统一 entity 包 |
| `service/domain/{activity,task,taskvehicle,vehicle}/repository/*` | `service/domain/repository/` | 合并到统一 repository 包 |
| `service/domain/task/service/*` | `service/domain/service/` | Task 领域服务 |
| `service/domain/factory/VehicleFactory` | `service/domain/factory/VehicleFactory` | |
| `service/infrastructure/repository/po/*` | `service/infrastructure/persistence/po/` | |
| `service/infrastructure/repository/dao/*` | `service/infrastructure/persistence/mapper/` | |
| `service/infrastructure/repository/impl/*` | `service/infrastructure/persistence/repository/` | |
| `service/infrastructure/repository/assembler/*` | `service/infrastructure/persistence/converter/` | |
| `service/infrastructure/exception/*` | `service/domain/exception/` | |
| `service/infrastructure/cache/*` | `service/infrastructure/cache/` | 保留 |
| `service/infrastructure/util/FotaHelper` | `service/infrastructure/util/FotaHelper` | 保留 |

### 三、包名替换

所有 `net.hwyz.iov.cloud.ota.fota` → `net.hwyz.iov.cloud.iov.ota`

### 四、外部依赖处理

fota 依赖的外部服务（需确认 iov-ota pom.xml 已包含或新增）：
- `baseline-api` — 基线服务（ExBaselineService）
- `pota-api` — 预设OTA服务（ExCompatiblePnService, ExFixedConfigWordService, ExSoftwareBuildVersionService）
- `vmd-api` — 车辆主数据服务

**注意**：这些依赖在 fota-service/pom.xml 中声明，迁移后需在 iov-ota-service/pom.xml 中添加。

---

## 执行顺序（按领域分阶段，每阶段验证编译）

```
Phase 1: 迁移 Activity 模块（活动管理）
Phase 2: 迁移 Task 模块（任务管理）
Phase 3: 迁移 TaskVehicle 模块（任务车辆进度）
Phase 4: 迁移 Vehicle 模块（车辆管理）
Phase 5: 迁移 Article 模块（文章管理）
Phase 6: 迁移 FotaCcp + Cache + Util + Exception
Phase 7: 合并 pom.xml 依赖 + SQL + 最终编译验证
```

每个阶段包含：
1. 复制源文件到目标路径
2. 批量替换包名和 import
3. 编译验证

---

## 关键设计决策

1. **Controller 直接实现**：Mpt Controller 去掉 `implements XxxMptApi`，直接实现业务逻辑
2. **ccp 包与 mpt/open 平级**：在 `service/adapter/web/controller/` 下新建 `ccp/` 包
3. **按领域逐步迁移**：每个领域迁移完后验证编译，不积累错误
4. **外部 Feign 依赖保留**：ExBaselineService 等外部服务调用保持不变
5. **SQL 合并**：fota 的建表语句合并到 `doc/sql/init.sql` 或新增 migration 文件
