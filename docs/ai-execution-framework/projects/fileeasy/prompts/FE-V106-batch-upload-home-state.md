你负责 FileEasy 的一个发布前修复任务。

## 任务身份

- Task ID: `FE-V106-batch-upload-home-state`
- Task name: `Fix batch upload homepage state so completion appears only after the whole batch finishes`
- Stage: `release-fix`

## 背景

PM 在验收中发现一个批量上传状态问题：

- 用户一次选择多个文件并开始上传
- 第 1 个文件上传完成后，服务端 APK 首页会切到“传输完成”
- 但此时第 2 个、第 3 个文件还没上传完，甚至可能还没有开始上传

这会让服务端首页误导管理员，以为整批传输已经结束。

PM 对现状的判断是：

- 上传页前端目前按顺序上传队列执行，一次只启动一个文件
- 服务端只有在某个文件执行 `upload/init` 后才知道这个上传会话存在
- 因此服务端首页只能看到“当前文件完成了”，看不到“同一批次里还有后续文件排队”
- 所以首页会在第一个文件完成到下一个文件初始化之间误判为“传输完成”

你的任务是修复这个状态判断，让服务端首页具备批次级视角。

## 任务目标

修复完成后必须满足：

- 批量选择多个合法文件并开始上传后，服务端首页能看到整批文件队列
- 只要批次里还有文件处于 queued / initialized / uploading / ready / receiving / paused / resumable 等未完成状态，服务端首页就保持“正在接收”
- 只有当前批次所有有效文件都 completed 后，服务端首页才切到“传输完成”
- 单文件上传行为不退化

## 非目标

不要做下面这些事：

- 不要改上传页视觉布局
- 不要恢复 `/admin`
- 不要新增远程文件管理、预览、删除、下载
- 不要修改文件类型白名单
- 不要修改 8MB 分片规则
- 不要修改 4GB 上限
- 不要处理“不同网络扫码后的强提示”
- 不要处理“上传记录入口”
- 不要处理 App icon 箭头问题

这些会单独开任务。

## 必读文档

1. `docs/FileEasy-PRD-v1.0.md`
2. `docs/FileEasy-Design-v1.0.md`
3. `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V106-batch-upload-home-state.json`
4. `docs/ai-execution-framework/projects/fileeasy/reports/FE-V105-Remove-Remote-Admin-Acceptance-Report.md`

## 允许修改范围

只能修改 task pack 中的 `allowedPaths`。

如果必须修改范围外文件，停止实现，不要越界，交给 PM 决策。

## 推荐修复方向

你可以自行选择实现方式，但必须能解释清楚机制。

推荐优先级：

1. 在上传页点击“开始上传”后，先为本次批量中所有合法文件创建 upload session，让服务端能看到完整队列。
2. 或引入明确的 batch id，把同一批次文件关联起来，并让首页按 batch 汇总状态。
3. 或采用其他等价机制，但必须保证服务端首页不会只根据“当前正在上传的单个文件”判断整个批次是否完成。

注意：

- 如果采用“批次开始时先 init 所有文件”，要避免重复创建已有 resumable task 的 session。
- 已经有 `uploadId` 的任务应继续复用原 session。
- 未支持类型、超过 4GB、初始化失败的任务不能让首页误以为还有有效文件待传。
- 批次暂停后，服务端首页不应显示“传输完成”；可显示“正在接收/等待继续/排队中”的现有等价状态。

## 你需要检查的代码区域

重点看：

- `apps/web-admin/src/fileeasy/pages/UploadPage.tsx`
  - `handleStartUpload`
  - `startUpload`
  - `batchRunning`
  - task 状态切换
- `apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt`
  - `initUploadSession`
  - `listUploadQueueSessions`
  - `buildHomeUploadTask`
  - `getHomeSummary`
- `apps/android-player/src/main/java/com/xplay/player/MainActivity.kt`
  - `FileEasyShellScreen`
  - `homeState`
  - `pendingUploads`
  - `completedUploads`

## 验收场景

必须覆盖并在交付说明中逐条说明：

### 场景 1：单文件

1. 选择 1 个合法文件
2. 开始上传
3. 上传中服务端首页显示“正在接收”
4. 上传完成后服务端首页显示“传输完成”

### 场景 2：多文件顺序上传

1. 选择 3 个合法文件
2. 开始上传
3. 第 1 个文件上传完成、第 2 个文件未完成时，服务端首页仍显示“正在接收”
4. 第 2 个文件上传完成、第 3 个文件未完成时，服务端首页仍显示“正在接收”
5. 第 3 个文件完成后，服务端首页才显示“传输完成”

### 场景 3：批次中断/暂停

1. 选择多个文件
2. 上传中暂停或网络中断
3. 服务端首页不得误显示“传输完成”

### 场景 4：异常文件

1. 批量选择中包含不支持类型或超过 4GB 文件
2. 无效文件应在上传页显示失败
3. 有效文件未全部完成前，服务端首页不得误显示“传输完成”

## 最低验证要求

至少执行：

```bash
npm --prefix apps/web-admin run build
```

Android 编译请在 Android 工程目录执行：

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=../../.gradle-home ./gradlew compileFileeasyDebugKotlin
```

如果有真机或浏览器条件，请补充手工验证：

- 批量选择 3 个小文件
- 观察服务端 APK 首页
- 第 1 个完成后截图或说明首页状态
- 第 2 个完成后截图或说明首页状态
- 全部完成后截图或说明首页状态

如果无法真机验证，不要宣称完整闭环通过，只能写“代码与构建通过，等待 PM 真机复验”。

## 交付格式

1. Completed work
- 本次完成了什么

2. Fix mechanism
- 是如何让服务端知道整个批次状态的

3. Changed files
- 改了哪些文件

4. Verification performed
- 做了哪些验证
- 哪些通过
- 哪些没法验证

5. Scenario result
- 单文件
- 多文件
- 暂停/中断
- 异常文件

6. Known risks
- 还剩哪些风险

7. PM handoff
- 是否建议 PM 真机复验
- 复验时重点看什么
