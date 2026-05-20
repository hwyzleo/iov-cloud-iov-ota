# FOTA 模块合并实施计划

> **For Hermes:** Use opencode skill to implement this plan task-by-task.

**Goal:** 将 iov-cloud-ota-fota 的全部代码按 DDD 分层结构合并到 iov-cloud-iov-ota 项目中，包名从 `net.hwyz.iov.cloud.ota.fota` 改为 `net.hwyz.iov.cloud.iov.ota`。

**Architecture:** 目标项目已有 DDD 分层结构（api/service），fota 代码按映射规则迁入对应层级。每个领域模块迁移后立即验证编译。

**Tech Stack:**
- Java 17 / Spring Boot / MyBatis-Plus
- MapStruct（Assembler/Converter）
- Lombok
- Maven 多模块（iov-ota-api + iov-ota-service）

**源路径:** `~/Projects/open-iov/iov-cloud-ota-fota`
**目标路径:** `~/Projects/open-iov/iov-cloud-iov-ota`

---

## Phase 1: 迁移 Activity 模块

### Task 1.1: 迁移 Activity VO（fota-api → iov-ota-api）

**Objective:** 将 Activity 相关的 VO 和枚举复制到 api/vo/

**Files:**
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/ActivityMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/ActivityAuditMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/ActivityCompatiblePnMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/ActivityFixedConfigWordMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/ActivitySoftwareBuildVersionMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/ActivityState.java`

**Step 1: 复制并修改包名**

对以下源文件执行复制 + 包名替换：
```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-api/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-api/src/main/java

# 复制 Activity VO
cp $SRC/net/hwyz/iov/cloud/ota/fota/api/contract/ActivityMpt.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/api/vo/ActivityMpt.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/api/contract/ActivityAuditMpt.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/api/vo/ActivityAuditMpt.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/api/contract/ActivityCompatiblePnMpt.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/api/vo/ActivityCompatiblePnMpt.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/api/contract/ActivityFixedConfigWordMpt.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/api/vo/ActivityFixedConfigWordMpt.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/api/contract/ActivitySoftwareBuildVersionMpt.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/api/vo/ActivitySoftwareBuildVersionMpt.java

# 复制枚举
cp $SRC/net/hwyz/iov/cloud/ota/fota/api/contract/enums/ActivityState.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/ActivityState.java
```

**Step 2: 批量替换包名和 import**

对所有新复制的文件执行：
```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

# 替换包声明
find iov-ota-api/src/main/java -name 'Activity*.java' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +

# 替换 import
find iov-ota-api/src/main/java -name 'Activity*.java' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +
```

**Step 3: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -pl iov-ota-api -q`
Expected: 编译通过（可能需要处理 cross-module import）

**Step 4: Commit**

```bash
git add iov-ota-api/
git commit -m "feat: 迁移 Activity 模块 VO 到 api/vo/"
```

---

### Task 1.2: 迁移 Activity Domain 模型

**Objective:** 将 Activity 领域模型合并到 domain/model/entity/

**Files:**
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ActivityDo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ActivitySoftwareBuildVersionVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ConfigWordVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwareBuildVersionDependencyVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwareBuildVersionVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwarePackageVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/ActivityRepository.java`

**Step 1: 复制源文件**

```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/java

# 复制 Activity model 到 entity
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/activity/model/ActivityDo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ActivityDo.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/activity/model/ActivitySoftwareBuildVersionVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ActivitySoftwareBuildVersionVo.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/activity/model/ConfigWordVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ConfigWordVo.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/activity/model/SoftwareBuildVersionDependencyVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwareBuildVersionDependencyVo.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/activity/model/SoftwareBuildVersionVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwareBuildVersionVo.java

cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/activity/model/SoftwarePackageVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwarePackageVo.java

# 复制 Activity Repository 接口
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/activity/repository/ActivityRepository.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/repository/ActivityRepository.java
```

**Step 2: 批量替换包名**

```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

FILES=(
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ActivityDo.java
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ActivitySoftwareBuildVersionVo.java
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/ConfigWordVo.java
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwareBuildVersionDependencyVo.java
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwareBuildVersionVo.java
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/SoftwarePackageVo.java
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/ActivityRepository.java
)

for f in "${FILES[@]}"; do
  sed -i '' 's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' "$f"
  sed -i '' 's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' "$f"
done
```

**Step 3: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -pl iov-ota-service -q 2>&1 | head -20`
Expected: 可能有外部依赖报错，但 Activity 相关文件无语法错误

**Step 4: Commit**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/
git commit -m "feat: 迁移 Activity 领域模型和 Repository 接口"
```

---

### Task 1.3: 迁移 Activity 基础设施层

**Objective:** 迁移 Activity 的 PO、Mapper、Repository 实现

**Files:**
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ActivityPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ActivityCompatiblePnPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ActivityFixedConfigWordPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ActivitySoftwareBuildVersionPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityMapper.java` (原 dao/ActivityDao)
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityCompatiblePnMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityFixedConfigWordMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivitySoftwareBuildVersionMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/ActivityRepositoryImpl.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/ActivityPoAssembler.java` (重命名自 assembler)

**Step 1: 复制源文件**

```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/java

# PO
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/ActivityPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ActivityPo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/ActivityCompatiblePnPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ActivityCompatiblePnPo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/ActivityFixedConfigWordPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ActivityFixedConfigWordPo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/ActivitySoftwareBuildVersionPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ActivitySoftwareBuildVersionPo.java

# Dao → Mapper
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/ActivityDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/ActivityCompatiblePnDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityCompatiblePnMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/ActivityFixedConfigWordDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityFixedConfigWordMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/ActivitySoftwareBuildVersionDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivitySoftwareBuildVersionMapper.java

# Repository Impl
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/ActivityRepositoryImpl.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/ActivityRepositoryImpl.java

# Assembler → Converter
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/assembler/ActivityPoAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/ActivityPoAssembler.java
```

**Step 2: 批量替换包名**

```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

# Activity 相关基础设施文件
find iov-ota-service/src/main/java -name 'Activity*.java' -path '*/persistence/*' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find iov-ota-service/src/main/java -name 'Activity*.java' -path '*/persistence/*' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +
```

**Step 3: Dao → Mapper 重命名**

打开 ActivityMapper.java（原 ActivityDao.java），将类名从 `ActivityDao` 改为 `ActivityMapper`，继承接口从 DAO 类型改为 MyBatis 的 `BaseMapper`（如果原 DAO 用的是不同基类）。

```bash
# 替换 Dao 类名为 Mapper
sed -i '' 's/class ActivityDao/class ActivityMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityMapper.java
sed -i '' 's/class ActivityCompatiblePnDao/class ActivityCompatiblePnMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityCompatiblePnMapper.java
sed -i '' 's/class ActivityFixedConfigWordDao/class ActivityFixedConfigWordMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivityFixedConfigWordMapper.java
sed -i '' 's/class ActivitySoftwareBuildVersionDao/class ActivitySoftwareBuildVersionMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ActivitySoftwareBuildVersionMapper.java
```

**Step 4: 更新 RepositoryImpl 中的 Mapper 引用**

检查 ActivityRepositoryImpl.java，确保引用的 Mapper 类名正确（ActivityMapper 等）。

**Step 5: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -pl iov-ota-service -q 2>&1 | head -30`
Expected: 可能有外部依赖缺失，但 Activity 相关文件无语法错误

**Step 6: Commit**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/
git commit -m "feat: 迁移 Activity 基础设施层（PO/Mapper/Repository/Converter）"
```

---

### Task 1.4: 迁移 Activity AppService + Controller + Assembler

**Objective:** 迁移 Activity 的应用层、Controller 层和 Assembler

**Files:**
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/service/ActivityAppService.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/ActivityMptController.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ActivityMptAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ActivityCompatiblePnMptAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ActivityFixedConfigWordMptAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ActivitySoftwareBuildVersionMptAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/BaselineSoftwareBuildVersionExServiceAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ConfigWordExServiceAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/SoftwareBuildVersionExServiceAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/SoftwareBuildVersionDependencyExServiceAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/SoftwarePackageExServiceAssembler.java`

**Step 1: 复制 AppService**

```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/java

cp $SRC/net/hwyz/iov/cloud/ota/fota/service/application/service/ActivityAppService.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/application/service/ActivityAppService.java
```

**Step 2: 复制 Controller（去掉 MptApi 接口）**

```bash
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/mpt/ActivityMptController.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/ActivityMptController.java

# 去掉 implements ActivityMptApi
sed -i '' 's/ implements ActivityMptApi//g' \
  $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/ActivityMptController.java
```

**Step 3: 复制所有 Activity 相关 Assembler**

```bash
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/ActivityMptAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ActivityMptAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/ActivityCompatiblePnMptAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ActivityCompatiblePnMptAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/ActivityFixedConfigWordMptAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ActivityFixedConfigWordMptAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/ActivitySoftwareBuildVersionMptAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ActivitySoftwareBuildVersionMptAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/BaselineSoftwareBuildVersionExServiceAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/BaselineSoftwareBuildVersionExServiceAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/ConfigWordExServiceAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ConfigWordExServiceAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/SoftwareBuildVersionExServiceAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/SoftwareBuildVersionExServiceAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/SoftwareBuildVersionDependencyExServiceAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/SoftwareBuildVersionDependencyExServiceAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/SoftwarePackageExServiceAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/SoftwarePackageExServiceAssembler.java
```

**Step 4: 批量替换包名**

```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

# Activity 相关 application 和 adapter 文件
find iov-ota-service/src/main/java -name 'Activity*.java' -path '*/application/*' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find iov-ota-service/src/main/java -name 'Activity*.java' -path '*/application/*' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +

find iov-ota-service/src/main/java -name 'Activity*.java' -path '*/adapter/*' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find iov-ota-service/src/main/java -name 'Activity*.java' -path '*/adapter/*' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +

# 其他 Assembler 文件
for f in iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/{Baseline*,ConfigWord*,SoftwareBuildVersion*,SoftwarePackage}*Assembler.java; do
  sed -i '' 's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' "$f"
  sed -i '' 's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' "$f"
done
```

**Step 5: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -pl iov-ota-service -q 2>&1 | head -40`

**Step 6: Commit**

```bash
git add iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/{application,adapter}/
git commit -m "feat: 迁移 Activity AppService + Controller + Assembler"
```

---

## Phase 2: 迁移 Task 模块

### Task 2.1: 迁移 Task VO + Domain + Infrastructure

**Objective:** 一次性迁移 Task 模块的所有层

**Files:**
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/TaskMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/TaskAuditMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskState.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskRestrictionType.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskStrategyType.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskPhase.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskType.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/UpgradeMode.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/UpgradeModeArg.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskDo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskRestrictionVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskStrategyVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/TaskRepository.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/service/TaskService.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskRestrictionPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskStrategyPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskRestrictionMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskStrategyMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/TaskRepositoryImpl.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/TaskPoAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/TaskRestrictionPoAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/TaskStrategyPoAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/service/TaskAppService.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/TaskMptController.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/TaskMptAssembler.java`

**Step 1: 复制所有 Task 相关文件**

使用脚本批量复制：
```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/java
APISRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-api/src/main/java
APIDEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-api/src/main/java

# Task VO
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/TaskMpt.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/TaskMpt.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/TaskAuditMpt.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/TaskAuditMpt.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/enums/TaskState.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskState.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/enums/TaskRestrictionType.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskRestrictionType.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/enums/TaskStrategyType.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskStrategyType.java

# Task domain enums（从 service/domain/contract/enums 迁到 api/vo/enums）
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/contract/enums/TaskPhase.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskPhase.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/contract/enums/TaskType.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskType.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/contract/enums/UpgradeMode.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/UpgradeMode.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/contract/enums/UpgradeModeArg.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/UpgradeModeArg.java

# Task Domain
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/task/model/TaskDo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskDo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/task/model/TaskRestrictionVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskRestrictionVo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/task/model/TaskStrategyVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskStrategyVo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/task/repository/TaskRepository.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/repository/TaskRepository.java

# Task Domain Service
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/task/service/TaskService.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/service/TaskService.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/task/service/impl/TaskServiceImpl.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/TaskServiceImpl.java

# Task PO
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/TaskPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskPo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/TaskRestrictionPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskRestrictionPo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/TaskStrategyPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskStrategyPo.java

# Task Dao → Mapper
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/TaskDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/TaskRestrictionDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskRestrictionMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/TaskStrategyDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskStrategyMapper.java

# Task Repository Impl
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/TaskRepositoryImpl.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/TaskRepositoryImpl.java

# Task Assembler → Converter
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/assembler/TaskPoAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/TaskPoAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/assembler/TaskRestrictionPoAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/TaskRestrictionPoAssembler.java

# Task AppService
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/application/service/TaskAppService.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/application/service/TaskAppService.java

# Task Controller
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/mpt/TaskMptController.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/TaskMptController.java

# Task Assembler
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/TaskMptAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/TaskMptAssembler.java
```

**Step 2: 批量替换包名**

```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

# 对 Task 相关文件全局替换包名和 import
find iov-ota-api/src/main/java -name 'Task*.java' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find iov-ota-api/src/main/java -name 'Task*.java' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +

find iov-ota-service/src/main/java -name 'Task*.java' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find iov-ota-service/src/main/java -name 'Task*.java' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +

# TaskServiceImpl 也需要替换
sed -i '' 's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/TaskServiceImpl.java
sed -i '' 's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/TaskServiceImpl.java
```

**Step 3: Dao → Mapper 重命名**

```bash
# Task Mapper 类名替换
sed -i '' 's/class TaskDao/class TaskMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskMapper.java
sed -i '' 's/class TaskRestrictionDao/class TaskRestrictionMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskRestrictionMapper.java
sed -i '' 's/class TaskStrategyDao/class TaskStrategyMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskStrategyMapper.java

# 去掉 implements TaskMptApi
sed -i '' 's/ implements TaskMptApi//g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/TaskMptController.java
```

**Step 4: 处理 TaskServiceImpl 包名**

TaskServiceImpl 原本在 `domain/task/service/impl/`，现在移到了 `infrastructure/persistence/repository/`，需要确认包声明和 import 正确。

**Step 5: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -q 2>&1 | head -50`

**Step 6: Commit**

```bash
git add .
git commit -m "feat: 迁移 Task 模块全部代码"
```

---

## Phase 3: 迁移 TaskVehicle 模块

### Task 3.1: 迁移 TaskVehicle 全部代码

**Objective:** 迁移 TaskVehicle（任务车辆进度）的所有层

**Files:**
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/TaskVehicleMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskVehicleState.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskVehicleDo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskVehicleSoftwareBuildVersionVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskVehicleStrategyVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/TaskVehicleRepository.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskVehiclePo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskVehicleDetailPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskVehicleProcessPo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleDetailMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleProcessMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/TaskVehicleRepositoryImpl.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/TaskVehiclePoAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/TaskVehicleProcessPoAssembler.java` (注意与现有重名冲突)
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/service/TaskVehicleAppService.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/TaskVehicleMptController.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/TaskVehicleMptAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/TaskVehicleProcessCcpAssembler.java`

**注意**：`VehStatusPoAssembler.java` 和 `VehStatusPo.java` 等 Vehicle 相关的基础设施文件已经在目标项目中存在（与 TaskVehicle 共用），需要合并处理。

**Step 1: 复制所有 TaskVehicle 相关文件**

```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/java
APISRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-api/src/main/java
APIDEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-api/src/main/java

# TaskVehicle VO
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/TaskVehicleMpt.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/TaskVehicleMpt.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/enums/TaskVehicleState.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/TaskVehicleState.java

# TaskVehicle Domain
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/taskvehicle/model/TaskVehicleDo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskVehicleDo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/taskvehicle/model/TaskVehicleSoftwareBuildVersionVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskVehicleSoftwareBuildVersionVo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/taskvehicle/model/TaskVehicleStrategyVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/TaskVehicleStrategyVo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/taskvehicle/repository/TaskVehicleRepository.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/repository/TaskVehicleRepository.java

# TaskVehicle PO
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/TaskVehiclePo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskVehiclePo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/TaskVehicleDetailPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskVehicleDetailPo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/TaskVehicleProcessPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/TaskVehicleProcessPo.java

# TaskVehicle Dao → Mapper
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/TaskVehicleDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/TaskVehicleDetailDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleDetailMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/TaskVehicleProcessDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleProcessMapper.java

# TaskVehicle Repository Impl
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/TaskVehicleRepositoryImpl.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/TaskVehicleRepositoryImpl.java

# TaskVehicle Assembler → Converter
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/assembler/TaskVehiclePoAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/TaskVehiclePoAssembler.java

# TaskVehicle AppService
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/application/service/TaskVehicleAppService.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/application/service/TaskVehicleAppService.java

# TaskVehicle Controller
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/mpt/TaskVehicleMptController.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/TaskVehicleMptController.java

# TaskVehicle Assembler
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/TaskVehicleMptAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/TaskVehicleMptAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/TaskVehicleProcessCcpAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/TaskVehicleProcessCcpAssembler.java
```

**Step 2: 批量替换包名**

```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

find iov-ota-api/src/main/java -name 'TaskVehicle*.java' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find iov-ota-api/src/main/java -name 'TaskVehicle*.java' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +

find iov-ota-service/src/main/java -name 'TaskVehicle*.java' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find iov-ota-service/src/main/java -name 'TaskVehicle*.java' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +

# 去掉 implements TaskVehicleMptApi
sed -i '' 's/ implements TaskVehicleMptApi//g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/TaskVehicleMptController.java

# Dao → Mapper
sed -i '' 's/class TaskVehicleDao/class TaskVehicleMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleMapper.java
sed -i '' 's/class TaskVehicleDetailDao/class TaskVehicleDetailMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleDetailMapper.java
sed -i '' 's/class TaskVehicleProcessDao/class TaskVehicleProcessMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/TaskVehicleProcessMapper.java
```

**Step 3: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -q 2>&1 | head -50`

**Step 4: Commit**

```bash
git add .
git commit -m "feat: 迁移 TaskVehicle 模块全部代码"
```

---

## Phase 4: 迁移 Vehicle 模块

### Task 4.1: 迁移 Vehicle 全部代码

**Objective:** 迁移 Vehicle（车辆管理）的所有层

**Files:**
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/VehicleMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/CloudFotaInfoCcp.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/DeviceInfoCcp.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/TaskVehicleProcessCcp.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/TaskVehicleStateCcp.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/VehicleFotaInfoCcp.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/CompatibleSoftwarePnMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/service/FotaCcpService.java` (原 FotaCcpApi)
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/fallback/FotaCcpServiceFallbackFactory.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/VehicleDo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/DeviceInfoVo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/repository/VehicleRepository.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/factory/VehicleFactory.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/VehStatusPo.java` (如与现有冲突则合并)
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/VehStatusMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/VehicleRepositoryImpl.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/VehStatusPoAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/service/VehicleAppService.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/VehicleMptController.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/ccp/FotaCcpController.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/VehicleMptAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/DeviceInfoCcpAssembler.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/TaskVehicleProcessCcpAssembler.java` (如已存在则跳过)
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/cache/CacheService.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/cache/impl/CacheServiceImpl.java`

**Step 1: 复制所有 Vehicle 相关文件**

```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/java
APISRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-api/src/main/java
APIDEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-api/src/main/java

# Vehicle VO
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/VehicleMpt.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/VehicleMpt.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/CloudFotaInfoCcp.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/CloudFotaInfoCcp.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/DeviceInfoCcp.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/DeviceInfoCcp.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/TaskVehicleProcessCcp.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/TaskVehicleProcessCcp.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/TaskVehicleStateCcp.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/TaskVehicleStateCcp.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/VehicleFotaInfoCcp.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/VehicleFotaInfoCcp.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/CompatibleSoftwarePnMpt.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/CompatibleSoftwarePnMpt.java

# FotaCcpService (原 FotaCcpApi)
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/feign/ccp/FotaCcpApi.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/service/FotaCcpService.java

# Vehicle Domain
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/vehicle/model/VehicleDo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/VehicleDo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/vehicle/model/DeviceInfoVo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/model/entity/DeviceInfoVo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/vehicle/repository/VehicleRepository.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/repository/VehicleRepository.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/domain/factory/VehicleFactory.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/factory/VehicleFactory.java

# Vehicle PO / Mapper / Repository
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/VehStatusPo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/VehStatusPo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/VehStatusDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/VehStatusMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/VehicleRepositoryImpl.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/VehicleRepositoryImpl.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/assembler/VehStatusPoAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/converter/VehStatusPoAssembler.java

# Vehicle AppService
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/application/service/VehicleAppService.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/application/service/VehicleAppService.java

# Vehicle Controller
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/mpt/VehicleMptController.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/VehicleMptController.java

# CCP Controller
mkdir -p $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/ccp/
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/ccp/FotaCcpController.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/ccp/FotaCcpController.java

# Vehicle/CCP Assembler
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/VehicleMptAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/VehicleMptAssembler.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/DeviceInfoCcpAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/DeviceInfoCcpAssembler.java

# Cache
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/cache/CacheService.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/cache/CacheService.java
mkdir -p $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/cache/impl/
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/cache/impl/CacheServiceImpl.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/cache/impl/CacheServiceImpl.java
```

**Step 2: 批量替换包名**

```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

# Vehicle 相关文件
find iov-ota-api/src/main/java -name 'Vehicle*.java' -o -name 'CloudFotaInfoCcp.java' -o -name 'DeviceInfoCcp.java' -o -name 'TaskVehicle*Ccp.java' -o -name 'FotaCcp*.java' -o -name 'CompatibleSoftwarePnMpt.java' | xargs -I {} sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {}
find iov-ota-api/src/main/java -name 'Vehicle*.java' -o -name 'CloudFotaInfoCcp.java' -o -name 'DeviceInfoCcp.java' -o -name 'TaskVehicle*Ccp.java' -o -name 'FotaCcp*.java' -o -name 'CompatibleSoftwarePnMpt.java' | xargs -I {} sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {}

find iov-ota-service/src/main/java -name 'Vehicle*.java' -o -name 'DeviceInfoVo.java' -o -name 'VehStatus*.java' -o -name 'Cache*.java' -o -name 'FotaCcp*.java' | xargs -I {} sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {}
find iov-ota-service/src/main/java -name 'Vehicle*.java' -o -name 'DeviceInfoVo.java' -o -name 'VehStatus*.java' -o -name 'Cache*.java' -o -name 'FotaCcp*.java' | xargs -I {} sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {}
```

**Step 3: 特殊处理**

```bash
# FotaCcpApi → FotaCcpService 重命名
sed -i '' 's/FotaCcpApi/FotaCcpService/g' \
  iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/service/FotaCcpService.java

# 去掉 implements VehicleMptApi
sed -i '' 's/ implements VehicleMptApi//g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/VehicleMptController.java

# Dao → Mapper
sed -i '' 's/class VehStatusDao/class VehStatusMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/VehStatusMapper.java
```

**Step 4: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -q 2>&1 | head -50`

**Step 5: Commit**

```bash
git add .
git commit -m "feat: 迁移 Vehicle + CCP + Cache 模块全部代码"
```

---

## Phase 5: 迁移 Article 模块

### Task 5.1: 迁移 Article 全部代码

**Objective:** 迁移 Article（文章管理）的所有层

**Files:**
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/ArticleMpt.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/AdaptiveLevel.java`
- Create: `iov-ota-api/src/main/java/net/hwyz/iov/cloud/iov/ota/api/vo/enums/AdaptiveSubject.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ArticlePo.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ArticleMapper.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/ArticleRepositoryImpl.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/application/service/ArticleAppService.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/ArticleMptController.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ArticleMptAssembler.java`

**Step 1: 复制所有 Article 相关文件**

```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/java
APISRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-api/src/main/java
APIDEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-api/src/main/java

# Article VO
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/ArticleMpt.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/ArticleMpt.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/enums/AdaptiveLevel.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/AdaptiveLevel.java
cp $APISRC/net/hwyz/iov/cloud/ota/fota/api/contract/enums/AdaptiveSubject.java \
   $APIDEST/net/hwyz/iov/cloud/iov/ota/api/vo/enums/AdaptiveSubject.java

# Article PO / Mapper / Repository
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/po/ArticlePo.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/po/ArticlePo.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/dao/ArticleDao.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ArticleMapper.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/repository/ArticleRepositoryImpl.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/repository/ArticleRepositoryImpl.java

# Article AppService
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/application/service/ArticleAppService.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/application/service/ArticleAppService.java

# Article Controller
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/mpt/ArticleMptController.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/ArticleMptController.java

# Article Assembler
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/facade/assembler/ArticleMptAssembler.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/adapter/web/assembler/ArticleMptAssembler.java
```

**Step 2: 批量替换包名**

```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

find . -name 'Article*.java' -path '*/iov-ota-api/*' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find . -name 'Article*.java' -path '*/iov-ota-api/*' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +

find . -name 'Article*.java' -path '*/iov-ota-service/*' -exec sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {} +
find . -name 'Article*.java' -path '*/iov-ota-service/*' -exec sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {} +

# 去掉 implements ArticleMptApi
sed -i '' 's/ implements ArticleMptApi//g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/adapter/web/controller/mpt/ArticleMptController.java

# Dao → Mapper
sed -i '' 's/class ArticleDao/class ArticleMapper/g' \
  iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/persistence/mapper/ArticleMapper.java
```

**Step 3: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -q 2>&1 | head -50`

**Step 4: Commit**

```bash
git add .
git commit -m "feat: 迁移 Article 模块全部代码"
```

---

## Phase 6: 迁移剩余文件

### Task 6.1: 迁移 Exception + Util + Mapper XML

**Objective:** 迁移异常类、工具类、MyBatis XML 映射文件

**Files:**
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/exception/ActivityNotExistException.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/exception/BaselineNotExistException.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/exception/TaskNotExistException.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/domain/exception/VehicleNotExistException.java`
- Create: `iov-ota-service/src/main/java/net/hwyz/iov/cloud/iov/ota/service/infrastructure/util/FotaHelper.java`
- Create: `iov-ota-service/src/main/resources/mappers/ActivityMapper.xml`
- Create: `iov-ota-service/src/main/resources/mappers/ArticleMapper.xml`
- Create: `iov-ota-service/src/main/resources/mappers/TaskMapper.xml`
- Create: `iov-ota-service/src/main/resources/mappers/TaskRestrictionMapper.xml`
- Create: `iov-ota-service/src/main/resources/mappers/TaskStrategyMapper.xml`
- Create: `iov-ota-service/src/main/resources/mappers/TaskVehicleMapper.xml`
- Create: `iov-ota-service/src/main/resources/mappers/TaskVehicleDetailMapper.xml`
- Create: `iov-ota-service/src/main/resources/mappers/TaskVehicleProcessMapper.xml`
- Create: `iov-ota-service/src/main/resources/mappers/VehStatusMapper.xml`

**Step 1: 复制文件**

```bash
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/java
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/java
XMLSRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/resources
XMLDEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/resources

# Exception
mkdir -p $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/exception/
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/exception/ActivityNotExistException.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/exception/ActivityNotExistException.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/exception/BaselineNotExistException.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/exception/BaselineNotExistException.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/exception/TaskNotExistException.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/exception/TaskNotExistException.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/exception/VehicleNotExistException.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/exception/VehicleNotExistException.java
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/exception/FotaBaseException.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/domain/exception/FotaBaseException.java

# Util
mkdir -p $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/util/
cp $SRC/net/hwyz/iov/cloud/ota/fota/service/infrastructure/util/FotaHelper.java \
   $DEST/net/hwyz/iov/cloud/iov/ota/service/infrastructure/util/FotaHelper.java

# MyBatis XML
mkdir -p $XMLDEST/mappers/
cp $XMLSRC/mappers/ActivityMapper.xml $XMLDEST/mappers/
cp $XMLSRC/mappers/ArticleMapper.xml $XMLDEST/mappers/
cp $XMLSRC/mappers/TaskMapper.xml $XMLDEST/mappers/
cp $XMLSRC/mappers/TaskRestrictionMapper.xml $XMLDEST/mappers/
cp $XMLSRC/mappers/TaskStrategyMapper.xml $XMLDEST/mappers/
cp $XMLSRC/mappers/TaskVehicleMapper.xml $XMLDEST/mappers/
cp $XMLSRC/mappers/TaskVehicleDetailMapper.xml $XMLDEST/mappers/
cp $XMLSRC/mappers/TaskVehicleProcessMapper.xml $XMLDEST/mappers/
cp $XMLSRC/mappers/VehStatusMapper.xml $XMLDEST/mappers/
```

**Step 2: 批量替换包名**

```bash
cd ~/Projects/open-iov/iov-cloud-iov-ota

# Exception 和 Util
find iov-ota-service/src/main/java -name '*NotExistException.java' -o -name 'FotaBaseException.java' -o -name 'FotaHelper.java' | xargs -I {} sed -i '' \
  's/package net\.hwyz\.iov\.cloud\.ota\.fota/package net.hwyz.iov.cloud.iov.ota/g' {}
find iov-ota-service/src/main/java -name '*NotExistException.java' -o -name 'FotaBaseException.java' -o -name 'FotaHelper.java' | xargs -I {} sed -i '' \
  's/import net\.hwyz\.iov\.cloud\.ota\.fota/import net.hwyz.iov.cloud.iov.ota/g' {}

# XML mapper namespace 替换
for f in iov-ota-service/src/main/resources/mappers/{Activity,Article,Task,TaskRestriction,TaskStrategy,TaskVehicle,TaskVehicleDetail,TaskVehicleProcess,VehStatus}Mapper.xml; do
  sed -i '' 's/net\.hwyz\.iov\.cloud\.ota\.fota/net.hwyz.iov.cloud.iov.ota/g' "$f"
  # DAO → Mapper 类名替换
  sed -i '' 's/Dao\.select/Mapper.select/g; s/Dao\.insert/Mapper.insert/g; s/Dao\.update/Mapper.update/g; s/Dao\.delete/Mapper.delete/g' "$f"
done
```

**Step 3: 验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn compile -q 2>&1 | head -50`

**Step 4: Commit**

```bash
git add .
git commit -m "feat: 迁移 Exception + Util + MyBatis XML 映射文件"
```

---

## Phase 7: 合并配置 + 最终验证

### Task 7.1: 合并 pom.xml 依赖

**Objective:** 在 iov-ota-service/pom.xml 中添加 fota 所需的外部依赖

**Files:**
- Modify: `iov-ota-service/pom.xml`

**Step 1: 添加缺失的依赖**

在 iov-ota-service/pom.xml 的 `<dependencies>` 中添加：
```xml
<!-- 框架: 审计（fota 需要） -->
<dependency>
    <groupId>net.hwyz.iov.cloud.framework</groupId>
    <artifactId>framework-audit-starter</artifactId>
</dependency>

<!-- 框架: 安全（fota 需要 @RequiresPermissions 等） -->
<dependency>
    <groupId>net.hwyz.iov.cloud.framework</groupId>
    <artifactId>framework-security-starter</artifactId>
</dependency>

<!-- 外部服务: baseline-api -->
<dependency>
    <groupId>net.hwyz.iov.cloud.ota</groupId>
    <artifactId>baseline-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- 外部服务: pota-api -->
<dependency>
    <groupId>net.hwyz.iov.cloud.ota</groupId>
    <artifactId>pota-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**Step 2: 合并配置文件**

```bash
# 合并 application.yml
SRC=~/Projects/open-iov/iov-cloud-ota-fota/fota-service/src/main/resources
DEST=~/Projects/open-iov/iov-cloud-iov-ota/iov-ota-service/src/main/resources

# 复制 bootstrap.yml 和 application.yml（如有差异则手动合并）
cp $SRC/bootstrap.yml $DEST/ 2>/dev/null || true
```

**Step 3: 合并 SQL**

```bash
# 检查 fota 是否有独立的 SQL 文件
ls ~/Projects/open-iov/iov-cloud-ota-fota/doc/sql/ 2>/dev/null

# 如果有，合并到目标项目的 init.sql
cat ~/Projects/open-iov/iov-cloud-ota-fota/doc/sql/*.sql >> \
  ~/Projects/open-iov/iov-cloud-iov-ota/doc/sql/init.sql
```

**Step 4: 最终编译验证**

Run: `cd ~/Projects/open-iov/iov-cloud-iov-ota && mvn clean compile -q 2>&1 | head -80`

Expected: 编译通过。如果有错误，逐个修复（主要是 import 路径、类名引用等）。

**Step 5: 提交**

```bash
git add .
git commit -m "feat: 合并 pom.xml 依赖 + 配置文件 + SQL，完成 FOTA 模块迁移"
```

---

## 验证清单

完成所有 Phase 后：
- [ ] `mvn clean compile` 编译通过
- [ ] 所有 `implements XxxMptApi` 已去掉
- [ ] 所有 `net.hwyz.iov.cloud.ota.fota` 已替换为 `net.hwyz.iov.cloud.iov.ota`
- [ ] 所有 Dao 类名已改为 Mapper
- [ ] 外部依赖（baseline-api, pota-api）已添加到 pom.xml
- [ ] XML mapper namespace 已更新
- [ ] ccp 包已创建并与 mpt/open 平级
- [ ] fota-api 内容已合并到 iov-ota-api
- [ ] fota-service 内容已合并到 iov-ota-service
