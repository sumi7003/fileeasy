# FileEasy Frontend Architecture v1.0

## 1. 文档信息

- 产品名：`FileEasy`
- 文档类型：`前端组件树与目录结构初始化方案`
- 版本：`v1.0`
- 状态：`开发初始化版`
- 上游文档：
  - [FileEasy-Development-Handoff-v1.0.md](/Users/jainaluo/易思态/code/Xplay/docs/FileEasy-Development-Handoff-v1.0.md)
  - [FileEasy-PRD-v1.0.md](/Users/jainaluo/易思态/code/Xplay/docs/FileEasy-PRD-v1.0.md)
  - [FileEasy-Design-v1.0.md](/Users/jainaluo/易思态/code/Xplay/docs/FileEasy-Design-v1.0.md)

## 2. 目标

本文件用于把 FileEasy 从“高保真原型”推进到“正式前端可开发结构”。

本次目标不是完成功能，而是完成以下前端架构准备：

- 正式目录落位
- 页面树冻结
- 组件树冻结
- hooks / services / types / constants 目录建立
- 原型页与正式页边界明确

## 3. 当前仓库现状

当前 `apps/web-admin/src` 目录主要包含：

- `pages/`
- `api/`
- `App.tsx`
- `main.tsx`

当前与 FileEasy 相关的现有资产：

- [FileEasyPrototypePage.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/FileEasyPrototypePage.tsx)
- [FileEasyUploadPage.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/FileEasyUploadPage.tsx)
- [fileeasy-prototype.css](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/fileeasy-prototype.css)
- [fileeasy.ts](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/api/fileeasy.ts)

结论：

- 原型页和已有上传页实现都应保留
- 正式结构不建议继续堆到 `src/pages/` 根层
- 建议在 `src/fileeasy/` 下建立正式业务域目录

## 4. 正式目录落位建议

建议正式目录落位为：

```text
apps/web-admin/src/fileeasy/
  components/
    admin/
    apk/
    shared/
    upload/
  constants/
  hooks/
  pages/
  services/
  types/
```

说明：

- `src/pages/FileEasyPrototypePage.tsx` 保留为设计原型页
- `src/fileeasy/pages/*` 作为正式业务页起点
- `src/api/fileeasy.ts` 暂不删除，后续逐步收口到 `src/fileeasy/services/*`

## 5. 页面树

### 5.1 正式页面

- `ApkHomePage`
- `UploadPage`
- `AdminPage`

### 5.2 页面职责

#### `ApkHomePage`

负责：

- 服务状态
- 服务阶段
- 上传二维码
- 上传地址
- 管理页入口
- 修改密码
- 首装说明
- 网络异常提示

不负责：

- 系统监控
- 日志查询
- 复杂运维信息

#### `UploadPage`

负责：

- 登录验证
- 文件选择
- 上传任务展示
- 上传进度
- 恢复上传
- 上传异常提示

不负责：

- 文件管理
- 文件删除
- 文件重命名

#### `AdminPage`

负责：

- 类型文件夹导航
- 文件搜索
- 文件列表
- 文件预览
- 文件重命名
- 文件删除
- 批量删除

不负责：

- 上传任务管理

## 6. 组件树

### 6.1 APK 端组件树

```text
apk/
  ApkHomeHeader
  ApkHomeMetrics
  ApkModeSwitcher
  ServiceStageStepper
  SetupChecklistCard
  QrDisplayCard
  QrUnavailableCard
  HomeActionList
  WelcomeDialog
  PasswordDialog
  NetworkHintDialog
```

### 6.2 上传页组件树

```text
upload/
  UploadPageHeader
  UploadFeatureChips
  UploadEntryCard
  UploadSummaryCard
  UploadTaskPanel
  UploadTaskCard
  UploadAlertBanner
  UploadLoginCard
```

### 6.3 管理页组件树

```text
admin/
  AdminHeader
  AdminMetricsBar
  AdminToolbar
  FolderSidebar
  FolderSidebarItem
  FilePaneHeader
  FileGrid
  FileCard
  FilePreviewDialog
  FileRenameDialog
  FileDeleteDialog
  BatchActionBar
  BatchFileRow
```

### 6.4 共享组件树

```text
shared/
  Button
  Input
  StatusPill
  FeatureChip
  MetricCard
  AlertCard
  EmptyState
  DialogShell
  ProgressBar
  PageSection
```

## 7. Hooks 规划

建议建立以下 hooks：

- `useAuthState`
  - 登录态恢复
  - 登录态失效
  - 登出

- `useUploadTasks`
  - 上传任务状态
  - 任务进度
  - 恢复上传
  - 本地持久化恢复

- `useFolderFilter`
  - 当前类型文件夹
  - 文件夹统计
  - 关键词搜索

## 8. Services 规划

### 8.1 `services/auth.ts`

负责：

- 登录
- 登出
- 登录态清理

### 8.2 `services/upload.ts`

负责：

- 上传初始化
- 分片上传
- 上传完成
- 上传状态查询
- 上传任务取消

### 8.3 `services/files.ts`

负责：

- 文件列表
- 文件详情
- 文件预览
- 文件下载
- 文件重命名
- 文件删除
- 批量删除

## 9. Types 规划

### 9.1 `types/auth.ts`

- `AuthSession`
- `LoginPayload`
- `LoginResponse`

### 9.2 `types/upload.ts`

- `UploadTask`
- `UploadTaskStatus`
- `UploadInitPayload`
- `UploadInitResponse`
- `UploadChunkPayload`
- `UploadStatusResponse`

### 9.3 `types/file.ts`

- `FileItem`
- `FileKind`
- `FolderKey`
- `PreviewKind`

### 9.4 `types/ui.ts`

- `StatusTone`
- `DialogState`
- `PageMode`

## 10. Constants 规划

### 10.1 `constants/routes.ts`

维护：

- `FILEEASY_UPLOAD_ROUTE`
- `FILEEASY_ADMIN_ROUTE`
- `FILEEASY_PROTO_ROUTE`

### 10.2 `constants/fileTypes.ts`

维护：

- 支持扩展名白名单
- 文件类型到文件夹映射
- 文件类型到预览能力映射

### 10.3 `constants/copy.ts`

维护：

- 状态文案
- 按钮文案
- 异常提示文案

## 11. 原型与正式代码边界

当前高保真原型文件：

- [FileEasyPrototypePage.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/FileEasyPrototypePage.tsx)

处理原则：

- 保留作为交互和视觉参考
- 不直接把 `PrototypePage` 重命名为正式页面
- 后续从其结构中逐步抽离正式组件

建议拆分顺序：

1. 先抽 `shared` 基础组件
2. 再抽 `upload` 业务组件
3. 再抽 `admin` 业务组件
4. 最后抽 `apk` 业务组件

原因：

- 现有仓库中已经存在上传页相关 API 和页面实现
- 上传页先抽离最容易复用现有成果

## 12. 初始化结果要求

本次初始化后，代码层应满足：

- `src/fileeasy/` 目录存在
- 页面、组件、hooks、services、types、constants 目录存在
- 每个目录至少有一个占位文件
- 构建不报错
- 原型页可继续使用

## 13. 推荐的下一步编码顺序

在本次初始化完成后，建议按以下顺序继续：

1. 补充 `types/*`
2. 抽离 `constants/*`
3. 抽离 `services/*`
4. 抽离 `useAuthState`
5. 抽离 `useFolderFilter`
6. 抽离 `useUploadTasks`
7. 建立 `UploadPage`
8. 建立 `AdminPage`
9. 建立 `ApkHomePage`

## 14. 结论

正式开发不应从“继续往 PrototypePage 塞代码”开始。

正确方式应是：

- 保留原型页
- 建立独立的 `src/fileeasy/` 正式结构
- 逐步从原型抽离为正式组件和页面

这样可以最大程度保留当前设计成果，同时避免正式代码被原型结构绑死。
