你负责 FileEasy 的一个发布阻塞修复任务。

## 任务身份

- Task ID: `FE-V104-remove-monitor-entry`
- Task name: `Release blocker fix - remove FileEasy monitor entry`
- Stage: `blocker-fix`

## 任务背景

这个任务不是常规功能开发，而是为了解决一个已经在 `FE-V102` 中确认过的发布阻塞项。

当前阻塞项是：

- `FileEasy APK shell still exposes a monitoring dashboard entry`
- 来源：`docs/ai-execution-framework/projects/fileeasy/reports/FE-V102-End-to-End-Integration-Validation-Report.md`
- 当前影响：
  - FileEasy 首页产品边界不再是纯服务端壳
  - 这与整体验收清单中的“无监控面板”要求冲突
  - 它阻塞 FileEasy v1 进入发布收口

## 任务目标

你只需要完成一个目标：

- 把 FileEasy 首页上的监控面板入口从 `blocking` 修到 `pass`

本任务的重点是：

1. 精准移除 FileEasy 壳中的监控入口
2. 不扩大到其他功能清理
3. 不破坏已有的上传地址、二维码、管理后台入口和改密入口
4. 给 PM 一个清晰的复验结果

## 非目标

你这次不要做下面这些事：

- 不要扩展产品功能
- 不要顺手重构监控模块
- 不要顺手改 unrelated UI
- 不要修改 PRD 或设计文档
- 不要把多个 blocker 混成一个任务

如果你发现当前阻塞项无法在既定范围内完成，请停止扩展，直接在交付说明里写明原因。

## 必读文档

1. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Overall-Acceptance-Report.md`
2. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Next-Phase-Plan.md`
3. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Integration-Checklist.md`
4. `docs/ai-execution-framework/projects/fileeasy/reports/FE-V102-End-to-End-Integration-Validation-Report.md`
5. `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V104-remove-monitor-entry.json`

## 允许修改范围

你只能修改下面路径：

- `apps/android-player/src/main/java/com/xplay/player/MainActivity.kt`

如果你认为必须改这个范围之外的文件，停止实现，不要越界，直接把原因写进交付说明。

## 你需要完成的工作

### 1. 修复当前阻塞项

目标场景：

- FileEasy 首页不应再出现“查看系统监控”入口
- FileEasy 首页不应再引导用户进入 `MonitorScreen`

修复完成后，应满足：

- 首页仍然保留：
  - 服务状态
  - 上传二维码
  - 上传地址
  - 管理后台入口
  - 修改密码入口
- 首页仍然不出现：
  - 播放器入口
  - 监控面板入口
  - 非 PRD 的额外后台入口

### 2. 保持范围边界

修复时必须同时保证：

- FileEasy 管理后台入口行为不被破坏
- FileEasy 上传地址与二维码展示逻辑不被破坏

### 3. 输出复验依据

你必须给出：

- 改动摘要
- 为什么这次改动能关闭当前 blocker
- 做了哪些验证
- 是否还有残留 blocker 风险

## 最低验证要求

至少执行并报告：

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin
```

如果条件允许，也请补充说明：

- 你如何确认 FileEasy 首页已不再暴露监控入口
- 哪些仍需用户或 PM 在真机上补测

## 交付时必须按这个格式返回

1. Completed work
- 本次完成了什么

2. Changed files
- 改了哪些文件

3. Verification performed
- 做了哪些验证
- 哪些通过
- 哪些没法验证

4. Blocker status
- 当前阻塞项是否已关闭
- 依据是什么

5. Known risks
- 还剩哪些风险

6. PM handoff
- 是否建议 PM 重新复验这个 blocker
- 是否还有后续 blocker 需要单独开任务
