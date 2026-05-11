# Xplay / FileEasy

本仓库当前承载两个产品方向：

- `Xplay`：原 Android 数字标牌 / 播放器系统。
- `FileEasy`：基于现有 Android 工程隔离出的局域网文件接收服务 APK。

当前活跃分支是 `fileeasy`，当前重点是推进 `FileEasy v1`。除非任务明确要求维护 Xplay，否则新开发、验收和 AI 派发都应以 FileEasy 文档为准。

## FileEasy 是什么

FileEasy 的目标是把一台 Android 手机、平板或盒子变成局域网文件接收节点。管理员安装 APK 后，APK 首页展示上传地址和二维码；手机、电脑等设备在同一 Wi-Fi 或热点局域网内扫码或输入地址，即可打开上传页并上传文件。

FileEasy v1 的产品边界：

- 纯服务端 APK，不带播放器功能。
- 仅支持局域网访问，包括 Wi-Fi 和热点。
- 不做公网访问，不做自动发现。
- 远程 Web 只保留上传页 `/`。
- v1 不提供远程 `/admin` 管理页。
- v1 不提供远程文件列表、预览、重命名、删除、批量删除、下载。

## 目录说明

```text
apps/android-player/
  Android 宿主工程。通过 product flavor 区分 xplay 与 fileeasy。

apps/web-admin/
  React/Vite Web 工程。FileEasy 上传页在这里实现，构建后同步到 Android assets。

docs/
  产品、设计、验收和 AI 开发流程文档。

docs/ai-execution-framework/
  AI 任务派发、范围校验、发布门禁和项目包规范。

docs/ai-execution-framework/projects/fileeasy/
  FileEasy 专属任务包、执行 prompt、PM checklist 和验收报告。

scripts/ai-framework/
  AI 开发流程校验脚本。
```

## 核心文档

- PRD：[docs/FileEasy-PRD-v1.0.md](docs/FileEasy-PRD-v1.0.md)
- 设计：[docs/FileEasy-Design-v1.0.md](docs/FileEasy-Design-v1.0.md)
- AI 流程：[docs/FileEasy-AI-Workflow-v1.0.md](docs/FileEasy-AI-Workflow-v1.0.md)
- FileEasy AI 项目包：[docs/ai-execution-framework/projects/fileeasy/README.md](docs/ai-execution-framework/projects/fileeasy/README.md)
- 当前阶段报告：[docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Overall-Acceptance-Report.md](docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Overall-Acceptance-Report.md)

## 环境要求

- Node.js `>= 18`
- npm 或 pnpm
- JDK 17
- Android SDK
- Gradle Wrapper 使用 `apps/android-player/gradlew`

本机常用 JDK 路径：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

## 开发与构建

安装前端依赖：

```bash
npm install
```

构建 Web 上传页：

```bash
npm --prefix apps/web-admin run build
```

编译 FileEasy APK：

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew compileFileeasyDebugKotlin
```

构建 FileEasy debug APK：

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew assembleFileeasyDebug
```

安装到已连接 Android 设备：

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew installFileeasyDebug
```

## FileEasy 使用说明

1. 安装并打开 FileEasy APK。
2. APK 首页启动本地文件服务，并展示上传地址与二维码。
3. 让上传设备连接到同一个 Wi-Fi 或当前热点。
4. 上传设备扫码或输入首页展示的地址。
5. 打开上传页 `/` 后输入密码登录。
6. 选择单个或多个文件上传。
7. 上传完成后，文件保存到 Android 设备本地私有目录。

默认能力：

- 支持单文件和多文件上传。
- 支持 `8MB` 分片上传。
- 支持断点续传。
- 单文件最大 `4GB`。
- 未完成上传会话保留 `24` 小时。
- 登录态保持 `7` 天。
- 修改密码仅允许在 APK 本机入口完成。

支持文件类型：

- 文档：`pdf doc docx xls xlsx ppt pptx txt`
- 图片：`jpg jpeg png gif webp`
- 视频：`mp4 mov`
- 音频：`mp3 wav m4a`
- 压缩文件：`zip`

## AI 开发流程

FileEasy 开发不直接从口头需求进入编码，必须按以下顺序推进：

1. 更新或确认 PRD。
2. 更新或确认设计文档。
3. 创建或更新 task pack。
4. 编写可派发给执行 AI 的 prompt。
5. 编写 PM acceptance checklist。
6. 执行 AI 按 prompt 实现。
7. PM 按 checklist 验收。
8. 验收通过后再发布或合并。

常用校验命令：

```bash
npm run ai:validate:bundle -- docs/ai-execution-framework/projects/fileeasy/project-bundle.json
npm run ai:validate:task -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json
npm run ai:check-scope -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json <changed-files...>
npm run ai:validate:publish -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json
```

## 当前状态

已完成并验收：

- `FE-T001`：FileEasy flavor 与纯服务端壳隔离。
- `FE-T002`：服务生命周期与局域网地址策略。
- `FE-T003`：上传后端与断点续传核心。
- `FE-T004`：远程上传页。
- `FE-CR001`：文档对齐，移除远程 `/admin` 管理页。
- `FE-V105`：代码侧移除远程 `/admin` 页面。

当前待返工：

- `FE-V106`：批量上传时 APK 首页状态不应提前切到完成态。核心修复方向正确，但混入了不同网络引导和接收记录入口，需要先拆出非本任务范围。

## 提交规范

- 不提交本地缓存，例如 `.gradle/`、`.gradle-home/`、`node_modules/`。
- 不提交无关构建中间产物，例如 `tsconfig.tsbuildinfo`。
- FileEasy 任务提交前至少运行：
  - `npm --prefix apps/web-admin run build`
  - `./gradlew compileFileeasyDebugKotlin`
  - 对应 task pack 的 `ai:validate:task`
  - 对应变更文件的 `ai:check-scope`
- 若需求变更，先改 PRD，再改设计，再派发任务，不允许执行 AI 自行扩范围。

## 旧 Xplay 说明

Xplay 仍保留在同一仓库中，Android flavor 为 `xplay`，包名为 `com.xplay.player`。FileEasy flavor 为 `fileeasy`，包名为 `com.xplay.fileeasy`。两个产品可以在同一工程中分别构建，但 FileEasy v1 不应暴露播放器能力。
