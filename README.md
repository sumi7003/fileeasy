# FileEasy / 易传输

FileEasy 是一个 Android 局域网文件接收工具。它可以把一台 Android 手机、平板或盒子变成局域网内的文件接收服务器，让手机、电脑等设备通过扫码或输入地址，在浏览器里直接上传文件。

这个仓库同时保留了历史 `Xplay` 数字标牌工程；当前主要维护方向是 `FileEasy`。

## 适合什么场景

- 临时把手机里的照片、视频、文档传到一台 Android 设备上。
- 在没有公网、没有云盘、没有数据线的局域网环境里传文件。
- 让 Android 平板、盒子、备用手机充当一个轻量文件接收节点。
- 在热点环境下，让其他设备连接热点后扫码上传。

FileEasy 不依赖云服务，不需要自动发现设备，不要求上传端安装 App。

## 核心能力

- Android APK 内置局域网 HTTP 文件服务。
- APK 首页展示上传地址和二维码。
- 上传端通过浏览器访问上传页 `/`。
- 支持密码登录，登录态保持 7 天。
- 支持单文件和多文件上传。
- 支持 `8MB` 分片上传。
- 支持中断或刷新后的断点续传。
- 单文件最大支持 `4GB`。
- 未完成上传会话保留 `24` 小时。
- 支持 Wi-Fi 和热点局域网场景。
- 上传完成的文件保存到系统下载目录下的 `Download/易传输/`。

## 当前边界

FileEasy v1 是纯文件接收服务，刻意保持简单：

- 不包含播放器功能。
- 不做公网访问。
- 不做自动设备发现。
- 不提供远程 `/admin` 管理页。
- 不提供远程文件列表、预览、重命名、删除、批量删除、下载。
- 不使用 `MANAGE_EXTERNAL_STORAGE` 全文件管理权限。

如果后续需要远程文件管理能力，会作为新版本单独设计。

## 支持文件类型

- 文档：`pdf`、`doc`、`docx`、`xls`、`xlsx`、`ppt`、`pptx`、`txt`
- 图片：`jpg`、`jpeg`、`png`、`gif`、`webp`
- 视频：`mp4`、`mov`
- 音频：`mp3`、`wav`、`m4a`
- 压缩文件：`zip`

## 使用方式

1. 安装并打开 FileEasy APK。
2. APK 首页会启动本地文件服务，并显示上传地址和二维码。
3. 让上传设备连接到同一个 Wi-Fi，或连接到当前 Android 设备开启的热点。
4. 上传设备扫码或在浏览器输入首页展示的地址。
5. 打开上传页后输入密码登录。
6. 选择一个或多个文件上传。
7. 上传完成后，在 Android 设备的 `Download/易传输/` 目录查看文件。

当前仓库还没有正式 Release 包。如需体验，需要从源码构建 APK。

## 从源码构建

环境要求：

- Node.js `>= 18`
- npm 或 pnpm
- JDK 17
- Android SDK

安装依赖：

```bash
npm install
```

构建 Web 上传页：

```bash
npm --prefix apps/web-admin run build
```

编译 FileEasy：

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew compileFileeasyDebugKotlin
```

构建 debug APK：

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew assembleFileeasyDebug
```

安装到已连接的 Android 设备：

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew installFileeasyDebug
```

## 工程结构

```text
apps/android-player/
  Android 宿主工程。通过 product flavor 区分 xplay 与 fileeasy。

apps/web-admin/
  React/Vite Web 工程。FileEasy 上传页在这里实现，构建后同步到 Android assets。

docs/
  FileEasy 产品、设计、交互、验收和 AI 开发流程文档。

docs/ai-execution-framework/
  AI 任务派发、范围校验、发布门禁和项目包规范。

scripts/ai-framework/
  AI 开发流程校验脚本。
```

## 关键文档

- [FileEasy PRD](docs/FileEasy-PRD-v1.0.md)
- [FileEasy Design](docs/FileEasy-Design-v1.0.md)
- [FileEasy Development Handoff](docs/FileEasy-Development-Handoff-v1.0.md)
- [FileEasy AI Workflow](docs/FileEasy-AI-Workflow-v1.0.md)
- [FileEasy AI Project Package](docs/ai-execution-framework/projects/fileeasy/README.md)

## 质量与协作

本项目使用文档驱动的 AI 协作流程。所有 FileEasy 任务都需要先有需求边界、设计文档、任务包和验收清单，再进入实现。

常用校验命令：

```bash
npm run ai:validate:bundle -- docs/ai-execution-framework/projects/fileeasy/project-bundle.json
npm run ai:validate:task -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json
npm run ai:check-scope -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json <changed-files...>
```

提交前建议至少运行：

```bash
npm --prefix apps/web-admin run build
```

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew compileFileeasyDebugKotlin
```

## 当前状态

FileEasy v1 正在开发和验证中，核心上传闭环已经具备：

- 独立 `fileeasy` flavor。
- APK 本地服务与上传二维码。
- 远程上传页。
- 密码登录。
- 分片上传与断点续传。
- 多文件上传。
- 远程 `/admin` 已从 v1 范围移除。

当前重点是收敛真实设备、热点/Wi-Fi、批量上传状态和发布前体验问题。正式 Release 包发布前，建议把当前版本视为开发预览版。

## Xplay 说明

`Xplay` 是本仓库的历史数字标牌 / 播放器方向，Android flavor 为 `xplay`，包名为 `com.xplay.player`。

`FileEasy` Android flavor 为 `fileeasy`，包名为 `com.xplay.fileeasy`。两个产品共用部分工程基础设施，但 FileEasy v1 不应暴露播放器能力。

## License

当前仓库尚未添加正式 `LICENSE` 文件。对外开源前，请先确认并补充许可证，例如 MIT、Apache-2.0 或其他适合项目的许可证。
