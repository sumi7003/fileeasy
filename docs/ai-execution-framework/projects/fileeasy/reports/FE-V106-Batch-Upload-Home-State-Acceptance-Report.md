# FE-V106 Batch Upload Home State Acceptance Report

## Conclusion

Status: **REWORK REQUIRED**

FE-V106 的核心方向是正确的：Web 上传页已经在批量开始时先为待上传文件初始化上传会话，Android 首页也改为只要存在非 completed 上传会话就保持 receiving 状态。因此，“批量上传时不要每完成一个文件就切到完成态”的主问题，在代码层面已有明确修复路径。

但是，本轮实现混入了 FE-V106 明确禁止的非目标内容，包括不同网络引导和接收记录入口。按照 FE-V106 PM checklist，本任务不能验收通过，必须先把范围收口。

## Verification

- Task pack validation: PASS
  - `npm run ai:validate:task -- docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V106-batch-upload-home-state.json`
- Scope path check: PASS
  - `npm run ai:check-scope -- docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V106-batch-upload-home-state.json ...`
- Web build: PASS
  - `npm --prefix apps/web-admin run build`
- Android compile: PASS
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=../../.gradle-home ./gradlew compileFileeasyDebugKotlin`
- Real-device batch upload validation: NOT VERIFIED in this review pass

## Accepted Parts

- Web 端批量上传启动逻辑已改为先准备整批上传会话，再进入实际上传队列。
- 已上传完成单个文件后，Web 页面不会仅凭单个任务 completed 就直接切到整体完成态。
- Android 首页状态判断已从“最近完成任务优先”调整为“存在未完成会话则保持 receiving”。
- Android 端上传队列查询已包含 completed 记录，便于首页判断当前批次是否整体完成。

## Blocking Findings

### P1: FE-V106 混入了不同网络引导，超出任务范围

FE-V106 的 non-goals 明确写了“不处理手机端不同网络后的失败/下一步指引提示”。但本轮实现改了远端上传页的网络不可用提示，并在 Android 首页加入了网络引导组件。

受影响位置：

- `apps/web-admin/src/fileeasy/pages/UploadPage.tsx`
- `apps/android-player/src/main/java/com/xplay/player/MainActivity.kt`

处理要求：

- 从 FE-V106 diff 中移除网络引导相关变更。
- 如果产品确认要做，应拆成独立任务，例如 `FE-V107-network-guidance`。

### P1: FE-V106 混入了接收记录入口，超出任务范围

FE-V106 的 non-goals 明确写了“不处理服务端首页上传记录入口”。但本轮实现增加了 `FileEasyRecordShortcut`，属于“首页接收记录入口”的需求。

受影响位置：

- `apps/android-player/src/main/java/com/xplay/player/MainActivity.kt`

处理要求：

- 从 FE-V106 diff 中移除接收记录入口相关变更。
- 如果产品确认要做，应拆成独立任务，例如 `FE-V108-upload-record-entry`。

## Required Rework

1. 只保留批量上传状态闭环相关改动。
2. 移除不同网络引导相关 UI、文案和组件。
3. 移除首页接收记录入口相关 UI、文案和组件。
4. 重新运行 Web build 和 Android compile。
5. 补充真实设备自测结果，至少覆盖：
   - 单文件上传：APK 首页 receiving -> completed。
   - 多文件批量上传：第 1 个文件完成后 APK 首页仍保持 receiving。
   - 多文件全部完成后：APK 首页才切换 completed。
   - 刷新上传页后继续上传：APK 首页状态不提前完成。

## PM Decision

FE-V106 暂不通过。

本轮不要求推翻批量上传状态方案，只要求把非 FE-V106 的需求剥离出去。范围收口后，如果构建和真实设备批量上传验证通过，可以进入下一轮验收。
