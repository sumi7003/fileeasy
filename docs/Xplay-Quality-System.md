# Xplay AI-TDD 质量保障体系

> 目标：把 Xplay 从“AI 可以直接改动巨石代码、质量靠回看和经验兜底”，升级为“红线可执行、契约可验证、改动可追踪、提交前可自动拦截”的工程体系。

**适用范围**:
- `apps/android-player`
- `apps/web-admin`
- Android Host Mode 主线

**不作为主线**:
- `apps/_deprecated_server`

**当前状态**: 方案设计，待分阶段实施

---

## 一、问题定义

Xplay 当前的主要质量风险不是“代码风格不统一”，而是下面 4 类高风险问题：

1. **安全红线风险**
   - 管理接口鉴权边界不清晰
   - 匿名访问范围过大
   - 会话模型不可信

2. **双端契约漂移**
   - `web-admin`、内置 Ktor 服务、Android 播放端对字段语义理解不一致
   - UI 文案和后端持久化行为可能不一致

3. **业务逻辑可运行但错误**
   - 排期逻辑、播放逻辑、更新逻辑出错时，往往不会立即崩溃
   - 这类问题最容易绕过肉眼 review

4. **巨石文件修改风险**
   - 核心逻辑集中在 [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt)
   - 局部改动容易误伤认证、播放列表、更新、设备注册等多个域

一句话总结：

**Xplay 现在最需要的不是“再加一点测试”，而是把高风险规则、跨端契约和关键链路变成自动守门系统。**

---

## 二、质量目标

### 2.1 终极目标

Xplay 中由 AI 编写或修改的代码，在提交前应当自动满足：

1. 没有违反安全和业务红线
2. 没有破坏双端契约
3. 没有影响关键播放/排期/更新链路
4. 需求可以通过测试表达并被人类审核

### 2.2 度量指标

| 指标 | 当前状态 | 目标 |
|------|---------|------|
| 红线自动覆盖率 | 低 | 100% 的核心红线有可执行测试 |
| Android 主线测试基座 | 缺失 | 可运行单测 + 集成测试 |
| Web Admin 表单契约测试 | 缺失 | 核心页面具备序列化与交互测试 |
| 提交前自动检查 | 缺失 | 高风险改动提交前自动拦截 |
| 巨石文件可测试边界 | 弱 | Auth/Schedule/Contract 可独立测试 |

---

## 三、适配 Xplay 的总体架构

```text
需求 / Bug / Review Finding
  ↓
测试先行（TDD）
  ↓
AI 写最小实现
  ↓
红线测试（P0）
  ↓
关键链路集成测试（P1）
  ↓
运行时不变式（P2）
  ↓
本地 Hook 自动守门（P3）
  ↓
CI 分层执行（P4）
  ↓
影响矩阵 + 复盘闭环（P5）
```

推荐分层：

- **P0 红线测试**：最优先，失败立即拦截
- **P1 集成测试**：验证模块间和双端链路
- **P2 运行时不变式**：运行期兜底
- **P3 Hook 自动守门**：防止 AI 忘记执行检查
- **P4 CI 分层**：红线优先、受影响测试优先
- **P5 TDD 工作流**：把测试作为需求规格

---

## 四、P0 - Xplay 红线转可执行测试

### 4.1 红线定义

第一批红线建议固定为 10 条：

| 编号 | 红线内容 | 风险来源 |
|------|---------|---------|
| `XR1` | 管理接口必须经过有效管理员会话 | 静态 cookie 可伪造 |
| `XR2` | 匿名访问仅限设备必要白名单 | 路由前缀整体放行 |
| `XR3` | 空排期必须表示全天 | UI/后端语义不一致 |
| `XR4` | 跨午夜排期必须正确生效 | 字符串比较不支持夜间窗口 |
| `XR5` | 设备名编辑不能在每次输入时触发重连 | UI 输入过程触发 `resetConnection()` |
| `XR6` | 双端 DTO 契约必须稳定 | 前端/服务端/客户端消费不一致 |
| `XR7` | 主线架构文档不得与当前实现严重漂移 | 文档仍描述旧的 NestJS / JWT / PostgreSQL 架构 |
| `XR8` | 禁止在业务代码中硬编码共享管理凭据 | 固定 cookie / 统一账密 / 默认口令 |
| `XR9` | 客户端内部管理能力不得通过伪造管理员身份访问接口 | 客户端请求直接带固定管理员身份信息 |
| `XR10` | 数据库迁移不得默认导致业务数据静默丢失 | `fallbackToDestructiveMigration()` 存在破坏性升级风险 |

### 4.2 建议测试目录

```text
apps/android-player/src/test/java/com/xplay/player/redlines/
├── RedlineAuthTest.kt
├── RedlineAnonymousAccessTest.kt
├── RedlineScheduleAllDayTest.kt
├── RedlineScheduleOvernightTest.kt
├── RedlineDeviceRenameTest.kt
├── RedlineContractConsistencyTest.kt
├── RedlineDocumentConsistencyTest.kt
├── RedlineCredentialHardcodeTest.kt
└── RedlineMigrationSafetyTest.kt
```

### 4.3 红线测试要求

- 测试名建议包含 `[RED LINE]`
- 每条红线至少有：
  - 正向测试：合规时允许通过
  - 反向测试：违规时必须失败
- 失败信息要能直接指向红线编号

### 4.4 红线与现有代码映射

| 红线 | 当前主要涉及文件 |
|------|----------------|
| `XR1` | [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt) |
| `XR2` | [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt) |
| `XR3` | [PlaylistManager.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/PlaylistManager.tsx), [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt) |
| `XR4` | [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt) |
| `XR5` | [MainActivity.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/MainActivity.kt), [DeviceRepository.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/DeviceRepository.kt) |
| `XR6` | `apps/web-admin/src/api/*`, [DeviceRepository.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/DeviceRepository.kt), [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt) |
| `XR7` | [docs/ARCHITECTURE.md](/Users/jainaluo/易思态/code/Xplay/docs/ARCHITECTURE.md), [README.md](/Users/jainaluo/易思态/code/Xplay/README.md), [docs/MASTER_PAD_DESIGN.md](/Users/jainaluo/易思态/code/Xplay/docs/MASTER_PAD_DESIGN.md) |
| `XR8` | [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt), [MonitorScreen.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/MonitorScreen.kt), [docs/trans.md](/Users/jainaluo/易思态/code/Xplay/docs/trans.md) |
| `XR9` | [MonitorScreen.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/MonitorScreen.kt), [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt) |
| `XR10` | [XplayDatabase.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/storage/XplayDatabase.kt), [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt) |

---

## 五、P1 - 关键集成测试

### 5.1 为什么 Xplay 特别需要集成测试

Xplay 的真实复杂度不在某个函数内部，而在这些“跨层链路”：

- Web Admin 表单 → API payload → Ktor route → Room/LocalStore
- Android 客户端注册 → 心跳 → 激活播放列表 → 播放器状态
- APK 上传 → 更新信息解析 → 设备下发 → 设备下载
- Host Mode 启动 → 本地服务 → WebView 管理页
- NSD 主机注册 → 从机发现 → `xplay.local` / IP 回退
- Room 升级 → 本地数据保留 / 回退策略

### 5.2 第一批关键链路

#### 链路 1：播放列表生命周期

```text
PlaylistManager.tsx
  → create/update payload
  → /api/v1/playlists
  → LocalStore.createPlaylist/updatePlaylist
  → heartbeat 激活
```

测试重点：
- 空时段
- 跨午夜
- 星期过滤
- 素材顺序与时长

#### 链路 2：设备注册与心跳

```text
DeviceRepository
  → /devices/register
  → /devices/{id}/heartbeat
  → active playlistIds
  → currentPlaylist
```

测试重点：
- 新设备注册
- 默认分配逻辑
- 时间过滤后的返回值
- 更新信息下发

#### 链路 3：管理员登录与访问控制

```text
login page
  → /auth/login
  → session / cookie
  → protected routes
```

测试重点：
- 登录成功
- 伪造 cookie 失败
- 未授权访问拦截

#### 链路 4：APK 更新链路

```text
Settings.tsx
  → /update/upload
  → getUpdateInfo()
  → heartbeat updateInfo
  → 设备下载
```

测试重点：
- 上传后版本识别
- 仅新版本触发下载
- 下载地址可用

#### 链路 5：NSD 发现与回退

```text
Host device
  → NSD register
  → client discover
  → discoveredIp / xplay.local 选择
  → 失败重试 / 手动 IP 回退
```

测试重点：
- 主机注册成功
- 从机能发现主机
- discovery 失败时重试
- 手动 IP 和自动发现不会互相污染

#### 链路 6：启动与生命周期

```text
BootReceiver
  → MainActivity
  → host mode / player mode 判断
  → LocalServerService 启停
  → 前台服务状态
```

测试重点：
- 开机自启
- host mode 启动/关闭
- 播放器和服务端双角色切换
- UI 状态与后台服务一致

#### 链路 7：Web Admin 资源初始化

```text
assets/web-admin
  → WebAdminInitializer
  → filesDir/web-admin
  → WebView / 浏览器访问
```

测试重点：
- 首次初始化成功
- 资源缺失时有清晰降级
- 已初始化状态可正确识别

#### 链路 8：文件中转开关与门禁

```text
docs/trans.md 需求
  → isTransferEnabled()
  → 菜单入口
  → 管理接口
  → 分享下载规则
```

测试重点：
- 功能关闭时入口与接口都不可见
- 启用前必须满足独立安全门禁
- 频控、过期、鉴权规则符合文档

### 5.3 建议目录

```text
apps/android-player/src/test/java/com/xplay/player/integration/
├── AuthFlowIntegrationTest.kt
├── PlaylistLifecycleIntegrationTest.kt
├── DeviceHeartbeatIntegrationTest.kt
├── UpdateFlowIntegrationTest.kt
├── DeviceDiscoveryIntegrationTest.kt
├── HostModeLifecycleIntegrationTest.kt
├── WebAdminBootstrapIntegrationTest.kt
└── TransferFeatureGateIntegrationTest.kt
```

前端建议：

```text
apps/web-admin/src/test/pages/
├── PlaylistManager.test.tsx
├── DeviceList.test.tsx
└── Settings.test.tsx
```

---

## 六、P2 - 运行时不变式

### 6.1 目的

测试负责开发期防错，不变式负责运行期兜底。

Xplay 不变式只应该加在最危险的入口，不应滥用。

### 6.2 建议实现

新增：

```text
apps/android-player/src/main/java/com/xplay/player/server/core/Invariants.kt
```

第一批不变式建议覆盖：

1. 管理接口访问前必须通过有效管理员会话
2. 匿名访问只能命中白名单路由
3. 排期空值必须保持“全天”语义
4. UI 输入态不得触发网络重连
5. 生产启用的共享凭据不得保持默认值
6. 高风险文档声明不得继续指向已废弃架构

### 6.3 行为建议

- 开发环境：抛异常，立即暴露问题
- 生产环境：记录 critical log，可选上报，不直接崩溃

---

## 七、P3 - 自动化守门

### 7.1 本地 Hook

建议新增：

```text
.githooks/
├── pre-commit
└── pre-push
```

### 7.2 策略

`pre-commit`
- 跑格式校验
- 跑受影响的最小测试集

`pre-push`
- 跑全部红线测试
- 跑核心集成测试

### 7.3 受影响测试分析

建议新增：

```text
tools/
├── run-affected-tests.js
└── impact-matrix.json
```

第一版映射示例：

- 改 [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt)
  - 跑全部 redlines
  - 跑 auth/playlist/update integration

- 改 [DeviceRepository.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/DeviceRepository.kt)
  - 跑 device heartbeat
  - 跑 rename redline
  - 跑 update flow

- 改 [PlaylistManager.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/PlaylistManager.tsx)
  - 跑页面测试
  - 跑 playlist integration

---

## 八、P4 - CI 分层

建议把 CI 按风险分层：

### Layer 1：红线测试

最先执行，失败立即拦截。

覆盖：
- `XR1-XR6`

### Layer 2：单元测试 + 集成测试

覆盖：
- auth
- schedule
- device heartbeat
- playlist lifecycle
- update flow

### Layer 3：覆盖率与质量门禁

建议逐步推进：
- 第一阶段不设硬门槛或设低门槛
- 后续提升核心模块覆盖率要求

### Layer 4：文档与配置一致性检查

覆盖：
- 主线架构文档是否仍引用旧架构作为当前事实
- `CONTRIBUTING.md` 中声明的 hook / test / 分支策略是否与仓库现状一致
- 关闭态功能是否在需求文档和代码开关上保持一致

---

## 九、P5 - 影响矩阵

建议新增：

```text
docs/engineering/IMPACT-MATRIX-XPLAY.md
```

示例矩阵：

| 文件 | Auth | Device | Playlist | Update | Web Admin | Player |
|------|------|--------|----------|--------|-----------|--------|
| `LocalServerService.kt` | ALL | ALL | ALL | ALL | ALL | Medium |
| `DeviceRepository.kt` | Low | ALL | High | High | None | ALL |
| `MainActivity.kt` | Medium | Medium | Low | Low | High | High |
| `PlaylistManager.tsx` | None | Low | ALL | None | ALL | Medium |
| `Settings.tsx` | Low | None | None | ALL | ALL | Medium |
| `NsdHelper.kt` | None | High | None | None | None | Medium |
| `BootReceiver.kt` | None | High | None | None | None | High |
| `WebAdminInitializer.kt` | None | None | None | None | High | Medium |
| `XplayDatabase.kt` | None | Medium | High | Medium | None | None |

用途：
- 改动前快速判断影响范围
- 驱动受影响测试脚本
- Code Review 检查回归范围

---

## 十、Xplay 的 AI 协作 TDD 工作流

### 10.1 工作流定义

```text
需求 / Bug / Review Finding
  ↓
AI 先写测试
  ↓
人类审核测试是否准确表达需求
  ↓
AI 写最小实现
  ↓
测试全绿
  ↓
必要时再重构
```

### 10.2 原则

1. 先审测试，不先审实现
2. 高风险逻辑必须先有失败测试
3. 一次改动只处理一个行为簇
4. 不直接在巨石文件里堆新逻辑，优先抽可测模块
5. 涉及主线架构、贡献流程、功能开关的改动，必须同步更新文档

### 10.3 哪些任务必须走 TDD

| 任务类型 | 是否必须 TDD |
|---------|-------------|
| 安全修复 | 必须 |
| 排期/播放逻辑修复 | 必须 |
| 双端 DTO 变更 | 必须 |
| 更新链路修复 | 推荐 |
| 纯 UI 微调 | 可选 |

---

## 十一、repo-local skills 规划

建议新增以下 skill：

### 11.1 `xplay-tdd-workflow`

用途：
- 定义 Xplay 仓库内的 AI 开发流程
- 先写失败测试，再写实现
- 每类改动完成后跑哪些验证

### 11.2 `xplay-host-mode-architecture`

用途：
- 说明当前主线是 Android Host Mode
- 明确 `android-player + web-admin + LocalServerService` 的职责
- 默认避免 AI 误改 `_deprecated_server`

### 11.3 `xplay-security-contracts`

用途：
- 维护匿名白名单
- 管理员接口边界
- 双端 DTO 契约

### 11.4 `xplay-impact-check`

用途：
- 按改动文件指引需要补哪些测试
- 减少“只改了一个点，漏回归整条链路”

### 11.5 `xplay-doc-consistency`

用途：
- 维护主线架构文档与真实实现的一致性
- 约束 `CONTRIBUTING.md`、`README.md`、需求文档中的自动化声明必须可执行
- 在架构迁移、功能启停、质量基线变化后提醒同步文档

---

## 十二、实施路线图

### Phase 1：先补最危险的点

- 为 `android-player` 接入测试基座
- 建立 `redlines/`
- 先落 `XR1-XR5`

验收标准：
- 当前 review 中的 5 个问题都能被测试先复现

### Phase 2：补关键链路集成测试

- Auth Flow
- Playlist Lifecycle
- Device Heartbeat
- Update Flow
- Device Discovery
- Host Mode Lifecycle
- Web Admin Bootstrap

验收标准：
- 至少 7 条高风险链路具备自动验证

### Phase 3：拆模块 + 上不变式

从 [LocalServerService.kt](/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt) 抽出：
- `server/auth`
- `server/schedule`
- `server/contracts`

验收标准：
- 红线逻辑不再散落在巨石文件中

### Phase 4：加 Hook 与 CI 分层

- 本地自动守门
- CI 红线优先
- 受影响测试优先
- 文档与配置一致性检查

验收标准：
- 高风险改动提交前自动拦截

### Phase 5：固化 AI-TDD 工作流

- 落地 skill
- 固化需求-测试模板
- 建立复盘闭环
- 建立文档同步清单

验收标准：
- 新的高风险需求默认先有测试再有实现

---

## 十三、第一批落地优先级

建议严格按下面顺序推进：

1. `android-player` 测试基座
2. `XR1-XR10` 红线测试
3. `auth` / `schedule` / `contracts` 从巨石文件中抽离
4. playlist / heartbeat / update / discovery / bootstrap 集成测试
5. `web-admin` 页面与表单测试
6. hook / CI / skill / doc-consistency

---

## 十四、结论

Xplay 当前最需要的，不是继续堆功能，也不是先做大规模重构，而是：

- 把高风险规则变成自动失败的测试
- 把跨端契约变成可回归的链路测试
- 把 AI 的“记得遵守规范”变成 hook 和 CI 的强制守门
- 把“文档写得对不对”也纳入质量系统，而不是只管代码

这套体系落地之后，Xplay 才真正具备可持续的 AI-TDD 开发基础。
