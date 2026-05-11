你负责 FileEasy 的一个发布阻塞修复任务。

## 任务身份

- Task ID: `FE-V104-wifi-reachability-stability`
- Task name: `Release blocker fix - stabilize same-WiFi LAN reachability`
- Stage: `blocker-fix`

## 任务背景

这个任务不是常规功能开发，而是为了解决 `FE-V103` 中已经确认过的发布阻塞项。

当前阻塞项是：

- `same-WiFi LAN reachability becomes flaky after initial success`
- 来源：`docs/ai-execution-framework/projects/fileeasy/reports/FE-V103-Real-Device-Network-Validation-Report.md`
- 当前影响：
  - 同 Wi-Fi 设备一开始能访问 `http://<device-ip>:3000/`、`/admin`、`/api/v1/ping`
  - 随后重复请求开始出现超时或无法连接
  - APK 首页仍显示服务运行，Android 侧也曾看到 `*:3000` 处于 `LISTEN`
  - 这会直接阻塞“局域网内扫码/输入地址上传文件”的核心验收

## 任务目标

你只需要完成一个目标：

- 把同 Wi-Fi 下 FileEasy HTTP 服务的稳定可达性从 `fail/blocking` 修到 `pass/non-blocking`

修复完成后，应满足：

- 另一个同 Wi-Fi 设备可以稳定访问 `http://<device-ip>:3000/api/v1/ping`
- 另一个同 Wi-Fi 设备可以稳定打开 `http://<device-ip>:3000/`
- APK 显示服务运行时，外部访问不应反复出现 timeout、connection refused、empty response
- 打开/关闭 APK 内置管理后台 WebView、App 前后台切换后，服务仍保持可达

## PM 读码提示

这些是 PM 读最新代码时看到的高风险线索，只作为排查起点，不要求你机械照改：

- `LocalServerService` 当前 `onStartCommand` 多处返回 `START_NOT_STICKY`
- `MainActivity.onStop()` 在 FileEasy 模式会调用 `LocalServerService.notifyAppBackground(this)`
- `LocalServerService.requestStopIfIdle()` 会在 App 后台且没有活跃上传会话时停止前台服务
- FileEasy PRD/设计要求它是局域网服务端 APK，服务运行期间应保持前台服务常驻，而不是因为 APK UI 进入后台就自动停止
- 当前 server 已使用 `embeddedServer(Netty, host = "0.0.0.0", port = 3000)`，所以如果端口仍 LISTEN 但外部访问失败，需要结合日志、生命周期、网络接口和 Android 约束继续定位

## 非目标

你这次不要做下面这些事：

- 不要修 admin 错误落到旧 Xplay 管理页的问题，除非你能证明它和同 Wi-Fi 不稳定是同一个根因
- 不要修登录后 `/api/v1/files` 返回 404 的问题，除非你能证明它和同 Wi-Fi 不稳定是同一个根因
- 不要做热点、网络切换、重启恢复的完整验收，这些后续单独派发
- 不要改 web-admin 源码
- 不要新增自动发现、外网访问、公网穿透、批量下载、日志页等范围外能力
- 不要修改 PRD 或设计文档
- 不要把播放器入口、监控面板、Xplay-only UI 带回 FileEasy

如果你发现当前阻塞项无法在既定范围内完成，请停止扩展，直接在交付说明里写明原因。

## 必读文档

1. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Overall-Acceptance-Report.md`
2. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Next-Phase-Plan.md`
3. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Integration-Checklist.md`
4. `docs/ai-execution-framework/projects/fileeasy/reports/FE-V103-Real-Device-Network-Validation-Report.md`
5. `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V104-wifi-reachability-stability.json`

## 允许修改范围

你只能修改下面路径：

- `apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt`
- `apps/android-player/src/main/java/com/xplay/player/utils/LanAddressResolver.kt`
- `apps/android-player/src/main/java/com/xplay/player/MainActivity.kt`
- `apps/android-player/src/main/java/com/xplay/player/BootReceiver.kt`
- `apps/android-player/src/main/AndroidManifest.xml`

如果你认为必须改这个范围之外的文件，停止实现，不要越界，直接把原因写进交付说明。

## 你需要完成的工作

### 1. 复现或定位阻塞项

先确认当前失败链路，至少关注：

- FileEasy APK 是否真的启动了前台服务
- Android 侧是否持续 `LISTEN` 在 `0.0.0.0:3000` 或等价地址
- APK 首页展示的 IP 是否是同 Wi-Fi 可访问 IP
- App 前后台切换、打开内置 WebView、无活跃上传会话时，服务是否被停止或变成不可达
- 失败时 logcat 中是否有 Ktor/Netty/service lifecycle 异常

### 2. 精准修复

修复方向由你的定位决定，但必须满足：

- FileEasy 服务运行期间保持 LAN 可达
- App UI 前后台变化不能让“正在运行的局域网服务端”意外停掉
- Runtime state、通知、首页状态不要出现“显示运行但外部不可达”的明显假阳性
- IP/二维码显示仍来自当前可用 LAN 接口，不要退回 `127.0.0.1`

### 3. 保持范围边界

修复时必须同时保证：

- FileEasy 仍是纯服务端 APK
- 不出现播放器入口
- 不出现监控面板入口
- 不引入自动发现或公网访问
- 不修改 web-admin 页面

### 4. 输出复验依据

你必须给出：

- 改动摘要
- 根因判断
- 为什么这次改动能关闭当前 blocker
- 做了哪些验证
- 哪些设备/网络场景仍需 PM 复验

## 最低验证要求

至少执行并报告：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin
```

如果有真机，请继续执行并报告：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew installFileeasyDebug
adb shell ss -ltnp | rg ':3000\b'
for i in {1..10}; do curl -sS -m 5 http://<device-ip>:3000/api/v1/ping; echo; sleep 1; done
curl -sS -m 5 -D - http://<device-ip>:3000/ | head
```

验证时请明确写：

- `<device-ip>` 是从哪里拿到的
- 同 Wi-Fi 访问测试是在电脑、另一台手机，还是 APK 内部 WebView 做的
- 是否测试过打开/关闭 APK admin WebView 后重复请求
- 是否测试过 App 切后台后重复请求

如果真机或同 Wi-Fi 条件不可用，不要宣称 blocker 已关闭，只能写“代码侧修复完成，等待 PM 真机复验”。

## 交付时必须按这个格式返回

1. Completed work
- 本次完成了什么

2. Root cause
- 你判断的根因是什么
- 证据是什么

3. Changed files
- 改了哪些文件

4. Verification performed
- 做了哪些验证
- 哪些通过
- 哪些没法验证

5. Blocker status
- 当前阻塞项是否已关闭
- 依据是什么

6. Known risks
- 还剩哪些风险

7. PM handoff
- 是否建议 PM 重新复验这个 blocker
- 如果 admin wrong-landing 或 `/api/v1/files` 404 仍存在，请作为独立 blocker 列出，不要混进本任务
