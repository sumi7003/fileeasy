# FileEasy 原型组件与正式业务组件边界清单 v1.0

## 1. 目的

这份清单用于约束 `apps/web-admin/src/fileeasy` 下两类代码的职责边界：

- `原型组件`
  只服务于 `fileeasy-demo`、设计评审、流程演示。
- `正式业务组件`
  服务于 `/`、`/admin`、后续正式 APK / Web 页面开发。

目标是避免后续继续演进时，把演示逻辑、假数据、场景切换状态混进正式业务实现里。

## 2. 命名规则

以下命名约定从现在开始固定：

- 组件名包含 `Prototype`
  代表原型专用组件，只能被 `FileEasyPrototypePage.tsx` 或其他明确的 demo 页面引用。
- 不含 `Prototype`
  代表正式组件，优先服务正式页面、真实接口、真实状态流。

## 3. 目录边界

### 3.1 原型组件

原型组件允许放在：

- `src/fileeasy/components/apk/*`
  仅当该组件同时能被正式 APK 页和 demo 复用时。
- `src/fileeasy/components/upload/*Prototype*`
- `src/fileeasy/components/admin/*Prototype*`

原型组件特点：

- 可以接受 demo 假数据
- 可以依赖 `jumpToScene` 一类演示跳转回调
- 可以内置“示意性”文案
- 不直接调用真实接口
- 不负责真实登录态、上传态、文件服务状态恢复

### 3.2 正式业务组件

正式业务组件放在：

- `src/fileeasy/components/shared`
- `src/fileeasy/components/upload`
- `src/fileeasy/components/admin`
- `src/fileeasy/components/apk`

正式业务组件特点：

- 不包含 `Prototype` 命名
- 不接受“场景切换”类 demo 回调
- 不依赖假数据结构
- 优先复用 `types/*`、`services/*`、`hooks/*`
- 面向真实页面与真实接口

## 4. 页面边界

### 4.1 原型页职责

`src/pages/FileEasyPrototypePage.tsx` 现在只应负责：

- 场景切换
- demo 假数据持有
- demo 弹窗开关
- 少量演示专用状态编排

它不应该再新增：

- 大段页面结构 DOM
- 可抽离的卡片、列表、整页场景结构
- 正式接口调用逻辑

### 4.2 正式页面职责

正式页面包括：

- `src/fileeasy/pages/UploadPage.tsx`
- `src/fileeasy/pages/AdminPage.tsx`
- `src/fileeasy/pages/ApkHomePage.tsx`

它们应该负责：

- 页面级状态
- 接口调用
- hooks / services 编排
- 将数据传给正式业务组件

它们不应该：

- 内联大段重复 UI
- 承担 demo 场景切换逻辑
- 引用 `Prototype` 组件，除非明确是 demo 过渡阶段

## 5. 当前组件归类

### 5.1 已归入原型组件

- `FileEasyUploadPrototypeLoginScene`
- `FileEasyUploadPrototypeActiveScene`
- `FileEasyUploadPrototypeAlertsScene`
- `FileEasyUploadPrototypeTaskCard`
- `FileEasyAdminPrototypeTopbar`
- `FileEasyAdminPrototypeFolderSidebar`
- `FileEasyAdminPrototypeFileGrid`
- `FileEasyAdminPrototypeBatchScene`
- `FileEasyAdminPrototypeFileActions`
- `FileEasyAdminPrototypePreviewDialog`
- `FileEasyAdminPrototypeRenameDialog`
- `FileEasyAdminPrototypeDeleteDialog`

### 5.2 已归入正式业务组件

- `FileEasyUploadHero`
- `FileEasyUploadSidePanel`
- `FileEasyUploadTasksSection`
- `FileEasyUploadTaskCard`
- `FileEasyAdminLoginGate`
- `FileEasyAdminHero`
- `FileEasyAdminControlsCard`
- `FileEasyAdminFilesSection`
- `FileEasyAdminPreviewDialog`
- `FileEasyAdminRenameDialog`
- `FileEasyAdminDeleteDialog`
- `FileEasyApkHomeScene`
- `FileEasyApkModeSwitcher`
- `FileEasyApkServiceStrip`
- `FileEasyApkQrCard`
- `FileEasyApkDialog`

## 6. 后续迁移规则

后续继续拆组件时，按下面顺序判断：

1. 这个组件是否要连接真实接口或真实状态？
  是：进正式业务组件。
2. 这个组件是否只用于 `fileeasy-demo` 的场景演示？
  是：进原型组件。
3. 这个组件是否未来会同时被 demo 和正式页复用？
  是：优先抽成正式通用展示组件，避免做两份。

## 7. 禁止事项

从现在开始，避免以下情况：

- 在 `FileEasyPrototypePage.tsx` 里继续新增整页结构
- 在正式页面里直接写 demo 文案如“示意”“演示密码”
- 在正式业务组件里接入 `scene`、`jumpToScene`
- 在原型组件里直接接真实接口
- 同一块 UI 同时维护“原型版”和“正式版”两套近似结构但不复用

## 8. 下一步建议

按当前状态，后续最合理的动作顺序是：

1. 继续把 `FileEasyPrototypePage.tsx` 中剩余顶部舞台区和少量通用提示继续抽离
2. 评估哪些 `Prototype` 组件已经足够稳定，可以升级成正式展示组件
3. 开始让 `ApkHomePage.tsx` 真正接入路由和正式入口，而不是仅保留演示态
