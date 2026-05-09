# FileEasy Development Handoff v1.0

## 1. 文档信息

- 产品名：`FileEasy`
- 文档类型：`开发前置清单与交接拆分文档`
- 版本：`v1.0`
- 状态：`开发前置版`
- 上游文档：
  - [FileEasy-PRD-v1.0.md](/Users/jainaluo/易思态/code/Xplay/docs/FileEasy-PRD-v1.0.md)
  - [FileEasy-Design-v1.0.md](/Users/jainaluo/易思态/code/Xplay/docs/FileEasy-Design-v1.0.md)
  - [FileEasy-Visual-Interaction-Spec-v1.0.md](/Users/jainaluo/易思态/code/Xplay/docs/FileEasy-Visual-Interaction-Spec-v1.0.md)
  - [FileEasy-Wireframe-Spec-v1.0.md](/Users/jainaluo/易思态/code/Xplay/docs/FileEasy-Wireframe-Spec-v1.0.md)
  - [FileEasy-Low-Fidelity-Page-Draft-v1.0.md](/Users/jainaluo/易思态/code/Xplay/docs/FileEasy-Low-Fidelity-Page-Draft-v1.0.md)

## 2. 文档目标

本文件用于回答两个问题：

1. 从当前高保真原型进入正式开发前，还必须先完成哪些前置工作
2. 正式开发阶段，页面、组件、接口、状态应如何拆分

本文件不替代 PRD 和技术设计文档，而是作为“启动开发前的统一交接件”。

## 3. 当前基线

当前已经具备以下基础资产：

- PRD 已定稿
- 技术设计文档已形成主干
- 视觉交互规范已形成
- 线框说明稿已形成
- 低保真页面草图说明已形成
- 高保真 HTML 可点击原型已接入 [App.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/App.tsx) 和 [FileEasyPrototypePage.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/FileEasyPrototypePage.tsx)

当前高保真原型的作用是：

- 对齐产品流程
- 对齐页面结构
- 对齐状态反馈
- 提供后续正式组件拆分参考

当前高保真原型不等于正式业务实现，仍然属于“开发前设计原型层”。

## 4. 开发前必须先定的事项

以下事项建议作为正式开发的前置门槛，未定之前不建议直接进入页面落地。

### 4.1 范围冻结

必须明确本轮正式开发仅覆盖：

- APK 首页
- 上传页
- `/admin` 管理页
- 首页相关弹窗
- 上传状态与异常提示
- 类型文件夹导航
- 文件预览
- 重命名
- 删除
- 批量删除

本轮不纳入：

- 批量下载
- 文档类在线预览
- 公网访问
- 自动设备发现
- 自动清理旧文件
- 复杂系统监控

### 4.2 正式前端落位方案

这是进入开发前最需要定的技术问题。

需要从以下方案中确认一种：

1. 继续基于 `apps/web-admin` 承载 FileEasy 前端
2. 在当前仓库中拆独立 `fileeasy-web` 前端应用
3. 保留当前原型在 `apps/web-admin`，正式版再拆独立 bundle 并由 Android 内嵌加载

建议结论：

- 若目标是快速推进并复用现有 Vite/React 能力，优先考虑“先在当前仓库内拆独立 FileEasy 页面与组件目录”
- 不建议把正式 FileEasy 长期耦合在当前 Xplay 管理台导航体系内部

### 4.3 路由与产物边界

需要确认以下正式路由边界：

- 上传页：`/`
- 管理页：`/admin`
- 预览层：页面内弹层，不新增独立路由
- 重命名、删除确认：页面内弹层，不新增独立路由

需要确认以下非正式原型路由不进入生产：

- `/fileeasy-demo`

### 4.4 接口契约冻结

在开发前，应把以下接口的请求/响应结构冻结：

- 登录
- 退出登录
- 上传初始化
- 上传分片
- 上传完成
- 上传状态查询
- 上传任务取消
- 文件列表
- 文件详情
- 文件预览
- 文件下载
- 文件重命名
- 文件删除
- 批量删除

接口冻结至少应包含：

- path
- method
- request schema
- response schema
- error schema
- 权限要求
- 前端状态映射方式

### 4.5 状态矩阵冻结

需要把所有页面状态一次性枚举清楚，避免开发时边写边猜。

必须冻结的状态见第 9 节。

### 4.6 验收口径冻结

需要在开发前确认“什么叫完成”，至少包括：

- 功能是否完成
- 页面是否对齐设计
- 状态是否齐全
- 移动端 / 桌面端是否可用
- 异常提示是否完整
- 登录态、断点续传、批量删除是否通过专项验证

## 5. 正式开发建议顺序

建议按以下顺序进入开发：

1. 先拆公共组件
2. 再拆 APK 首页
3. 再拆上传页
4. 再拆管理页
5. 再接入真实接口
6. 最后补齐空状态、异常态、动效和验收联调

不建议的顺序：

- 直接把原型页整体改造成正式页
- 先接接口再补结构
- 页面写完后再补状态矩阵

## 6. 页面与模块拆分

### 6.1 正式页面树

建议正式页面树如下：

1. `ApkHomePage`
2. `UploadLoginPage`
3. `UploadWorkspacePage`
4. `AdminFileManagerPage`

### 6.2 页面内模块树

#### `ApkHomePage`

- `ApkHomeHeader`
- `ApkHomeStatusPanel`
- `ApkHomeServiceProgress`
- `ApkHomeSetupCard`
- `ApkHomeQrCard`
- `ApkHomeActionList`
- `ApkWelcomeDialog`
- `ApkChangePasswordDialog`
- `ApkNetworkHintDialog`

#### `UploadLoginPage`

- `UploadPageHeader`
- `UploadFeatureChips`
- `UploadEntryCard`
- `UploadLoginCard`
- `UploadGhostTaskPanel`

#### `UploadWorkspacePage`

- `UploadPageHeader`
- `UploadFeatureChips`
- `UploadActionPanel`
- `UploadSummaryPanel`
- `UploadTaskPanel`
- `UploadTaskCard`
- `UploadAlertBanner`

#### `AdminFileManagerPage`

- `AdminHeader`
- `AdminMetricsBar`
- `AdminToolbar`
- `FolderSidebar`
- `FolderItem`
- `FilePaneHeader`
- `FileGrid`
- `FileCard`
- `FilePreviewDialog`
- `FileRenameDialog`
- `FileDeleteDialog`
- `BatchDeleteBar`
- `BatchFileRow`

## 7. 组件拆分建议

### 7.1 基础组件

建议优先抽出：

- `Button`
- `Input`
- `StatusPill`
- `MetricCard`
- `FeatureChip`
- `AlertCard`
- `EmptyState`
- `DialogShell`
- `ProgressBar`
- `Toast`

### 7.2 业务组件

建议在基础组件之上抽出：

- `QrDisplayCard`
- `ServiceStageStepper`
- `UploadTaskCard`
- `FileCard`
- `FolderSidebar`
- `BatchFileRow`
- `PasswordForm`

### 7.3 不建议抽象过度的部分

以下部分在第一版不建议过早抽成高度通用组件：

- APK 首页三种状态卡
- 上传异常面板
- 文件预览内容区

这些部分可以先保持页面内业务组件形态，等第二轮复用稳定后再提升抽象。

## 8. 路由树与目录建议

### 8.1 推荐目录结构

建议在前端内形成以下结构：

```text
src/
  fileeasy/
    pages/
      ApkHomePage.tsx
      UploadPage.tsx
      AdminPage.tsx
    components/
      apk/
      upload/
      admin/
      shared/
    hooks/
      useAuthState.ts
      useUploadTasks.ts
      useFolderFilter.ts
    services/
      auth.ts
      upload.ts
      files.ts
    types/
      auth.ts
      upload.ts
      file.ts
      ui.ts
    constants/
      routes.ts
      fileTypes.ts
      copy.ts
```

### 8.2 路由树建议

```text
/
  -> UploadPage
/admin
  -> AdminPage
```

说明：

- APK 首页不属于 Web 路由，它属于 Android 壳层页面
- Web 端只承载上传页和管理页
- 预览、重命名、删除确认均为页面内弹层

## 9. 状态矩阵

### 9.1 APK 首页状态

- `first-install`
- `booting`
- `foreground-running`
- `ready`
- `network-missing`

每个状态都必须定义：

- 标题
- 辅助文案
- 二维码是否显示
- 地址是否显示
- 哪个主操作可用
- 是否弹出引导 / 提示弹窗

### 9.2 上传页状态

- `login-idle`
- `login-error`
- `upload-empty`
- `upload-queued`
- `uploading`
- `restoring`
- `upload-done`
- `upload-failed`
- `space-insufficient`
- `network-interrupted`
- `resume-expired`

### 9.3 管理页状态

- `list-default`
- `list-empty`
- `search-empty`
- `folder-filtered`
- `preview-open`
- `rename-open`
- `delete-open`
- `batch-mode`
- `batch-empty`

## 10. 接口拆分清单

以下为前端正式开发所需接口清单。

### 10.1 认证接口

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`

前端关注点：

- 登录失败错误提示文案
- 登录态保持 7 天
- 密码变更后强制失效

### 10.2 上传接口

- `POST /api/v1/upload/init`
- `POST /api/v1/upload/chunk`
- `POST /api/v1/upload/complete`
- `GET /api/v1/upload/status/{uploadId}`
- `DELETE /api/v1/upload/status/{uploadId}`

前端关注点：

- 单文件最大 4GB 校验
- 分片固定 8MB
- 续传状态保留 24 小时
- 网络中断后的恢复策略

### 10.3 文件管理接口

- `GET /api/v1/files`
- `GET /api/v1/files/{id}`
- `GET /api/v1/files/{id}/preview`
- `GET /api/v1/files/{id}/download`
- `PATCH /api/v1/files/{id}`
- `DELETE /api/v1/files/{id}`
- `POST /api/v1/files/batch-delete`

说明：

- 批量删除接口命名可在技术实现时微调
- 但必须在正式开发前冻结能力和返回结构

## 11. 前端数据模型建议

### 11.1 上传任务模型

建议字段：

- `uploadId`
- `fileName`
- `fileSize`
- `fileType`
- `progress`
- `status`
- `uploadedBytes`
- `resumable`
- `errorMessage`
- `updatedAt`

### 11.2 文件列表模型

建议字段：

- `id`
- `name`
- `extension`
- `kind`
- `size`
- `previewable`
- `createdAt`
- `updatedAt`
- `downloadUrl`
- `previewUrl`

### 11.3 文件夹导航模型

建议字段：

- `folderKey`
- `label`
- `extensions`
- `count`

说明：

- v1 可以由前端按扩展名映射生成文件夹
- 若后端后续直接返回分组统计，前端可切换为以后端数据为准

## 12. 文案与设计资产前置

进入正式开发前，建议同时产出以下资产：

1. 文案状态表
2. 图标映射表
3. 文件类型到文件夹映射表
4. 颜色与状态语义表
5. 弹窗文案表

若这些资产缺失，开发阶段最容易出现：

- 同一状态多种文案
- 图标风格不统一
- 文件夹分类口径不一致
- 设计和开发实现出现偏差

## 13. 当前高保真原型到正式代码的映射关系

当前原型参考文件：

- [App.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/App.tsx)
- [FileEasyPrototypePage.tsx](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/FileEasyPrototypePage.tsx)
- [fileeasy-prototype.css](/Users/jainaluo/易思态/code/Xplay/apps/web-admin/src/pages/fileeasy-prototype.css)

建议映射方式：

- 保留原型作为参考页，不直接演进为正式业务页
- 参考其结构拆成正式页面与组件
- 正式组件不应继续堆叠在单一 `PrototypePage` 中

## 14. 启动开发前检查清单

正式开发前，请逐项确认：

- PRD 范围已冻结
- 技术落位方案已确认
- 路由树已确认
- 页面树已确认
- 组件树已确认
- 接口清单已冻结
- 状态矩阵已冻结
- 文案状态表已完成
- 文件夹分类口径已确认
- 验收标准已确认

若以上未完成，不建议直接进入大规模页面编码。

## 15. 推荐的下一步执行动作

建议严格按以下顺序继续：

1. 确认正式前端落位方案
2. 输出接口与状态枚举文档
3. 输出正式组件树与页面树
4. 建立正式目录结构
5. 从原型拆出共享组件
6. 正式开发 APK 首页
7. 正式开发上传页
8. 正式开发管理页
9. 接入真实接口联调
10. 补齐验收与缺陷修复

## 16. 结论

当前 FileEasy 已经具备进入正式开发的设计基础，但还不建议直接“从原型页开始写业务”。

最正确的做法是：

- 先完成开发前置冻结
- 再按页面树和组件树拆分
- 最后进入正式编码

这样可以最大程度降低返工和结构性重写成本。
