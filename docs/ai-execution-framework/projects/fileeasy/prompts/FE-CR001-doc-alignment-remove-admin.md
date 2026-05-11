你负责 FileEasy 的需求变更文档对齐任务。

## 任务身份

- Task ID: `FE-CR001-doc-alignment-remove-admin`
- Task name: `Requirement change alignment - remove remote admin page from FileEasy v1`
- Stage: `change-request-doc-alignment`

## 背景

PM 已决定：FileEasy v1 砍掉远程管理页面 `/admin`。

新的 v1 边界是：

- 远程用户只访问上传页 `/`
- 远程用户只负责登录和上传
- APK 本机保留服务状态、上传地址、二维码、密码修改等本机控制
- 远程预览、重命名、删除、批量删除、下载、文件列表管理全部移出 v1

请先阅读：

1. `docs/ai-execution-framework/projects/fileeasy/change-requests/FE-CR001-remove-remote-admin-page.md`
2. `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-CR001-doc-alignment-remove-admin.json`
3. `docs/FileEasy-PRD-v1.0.md`
4. `docs/FileEasy-Design-v1.0.md`
5. `docs/FileEasy-AI-Workflow-v1.0.md`

## 允许修改范围

你只能修改 task pack 中列出的文档路径。

不要改任何代码。

## 你要完成什么

### 1. 更新 PRD

把 v1 产品范围改成：

- APK 服务端壳
- 远程上传页 `/`
- 上传登录、分片上传、断点续传、文件类型限制、4GB 上限、空间不足处理
- APK 本机密码修改

从 v1 移除：

- `/admin`
- 远程文件列表
- 远程预览
- 远程重命名
- 远程删除
- 批量删除
- 远程下载

### 2. 更新设计文档

设计文档必须明确：

- `/` 是唯一远程用户页面
- `/admin` 不再是 v1 路由
- FileEasy 模式下远程文件管理 API 不应暴露
- 上传页 API 边界保留
- APK 本机入口不再跳转远程 admin WebView
- 后续如果要做文件管理，应作为 v1.1 或单独需求重新设计

### 3. 更新流程/视觉/线框/验收文档

把所有仍要求远程管理页的内容改掉。

验收方向应回到：

- 上传页
- 密码登录
- 多文件上传
- 8MB 分片
- 断点续传
- 4GB 上限
- 同 Wi-Fi 稳定性
- 热点访问
- 网络切换
- 前台服务常驻
- 开机自启

### 4. 不要隐藏历史问题

如果报告里出现之前的 `/admin` blocker，可以保留为历史背景，但必须标注：

- 该 blocker 因 FE-CR001 范围变更而关闭或降级
- 后续不再以远程 admin 管理能力作为 v1 验收项

## 最低自测

执行并报告：

```bash
rg -n "/admin|管理页|管理后台|预览|重命名|批量删除|下载" docs/FileEasy-PRD-v1.0.md docs/FileEasy-Design-v1.0.md docs/FileEasy-AI-Workflow-v1.0.md docs/FileEasy-Visual-Interaction-Spec-v1.0.md docs/FileEasy-Wireframe-Spec-v1.0.md docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Integration-Checklist.md docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Next-Phase-Plan.md docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Overall-Acceptance-Report.md
```

注意：这个命令有命中不一定失败。你需要逐条判断命中是否仍在要求远程 admin，还是已经作为“已移出范围/历史背景”存在。

## 交付格式

1. Completed work
- 更新了哪些文档

2. Scope changes
- v1 删除了哪些远程管理能力
- v1 保留了哪些上传核心能力

3. Verification performed
- 执行了哪些检查
- 还有哪些关键词命中以及原因

4. Implementation handoff
- FE-V105 实现任务是否可以开始
- 实现 AI 需要注意哪些文档变化

5. Risks
- 还有哪些需求歧义或历史文档残留
