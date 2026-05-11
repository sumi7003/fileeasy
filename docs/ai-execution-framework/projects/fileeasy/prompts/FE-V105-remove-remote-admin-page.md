你负责 FileEasy 的一个范围变更实现任务。

## 任务身份

- Task ID: `FE-V105-remove-remote-admin-page`
- Task name: `Scope change implementation - remove remote admin page from FileEasy v1`
- Stage: `scope-change-implementation`

## 前置条件

必须先确认 `FE-CR001-doc-alignment-remove-admin` 已完成或 PM 明确允许你基于当前变更单先做实现。

必读：

1. `docs/ai-execution-framework/projects/fileeasy/change-requests/FE-CR001-remove-remote-admin-page.md`
2. `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V105-remove-remote-admin-page.json`
3. `docs/FileEasy-PRD-v1.0.md`
4. `docs/FileEasy-Design-v1.0.md`
5. `docs/ai-execution-framework/projects/fileeasy/reports/FE-V103-Real-Device-Network-Validation-Report.md`

## 背景

PM 已决定：FileEasy v1 不再提供远程管理页 `/admin`。

新的 v1 目标是先把“局域网扫码/输入地址上传文件到 APK 服务端”做稳。

所以你要删除或禁用远程管理面：

- 不再有远程 `/admin` 页面
- 不再有远程文件列表
- 不再有远程预览
- 不再有远程重命名
- 不再有远程删除/批量删除
- 不再有远程下载管理入口

但必须保留上传核心：

- `/` 上传页
- 密码登录
- 文件类型校验
- 8MB 分片
- 4GB 上限
- 断点续传
- 上传完成反馈

## 允许修改范围

你只能修改 task pack 中的 `allowedPaths`。

如果你认为必须修改范围外文件，停止实现，不要越界，写明原因交给 PM。

## 你要完成什么

### 1. 移除 APK 里的远程 admin 入口

FileEasy 模式下：

- APK 首页不要显示“管理后台”“文件管理后台”这类远程 admin 入口
- 不要打开 `/admin` WebView
- 上传地址、二维码、服务状态、密码修改入口仍保留

注意：Xplay 非 FileEasy 模式不要被破坏。

### 2. 移除或禁用 Web `/admin`

远程访问：

- `http://<device-ip>:3000/` 仍是上传页
- `http://<device-ip>:3000/admin` 不得显示文件管理 UI
- `/admin` 可以返回 `404`、`410`，或跳回 `/`
- 你必须在交付说明中明确最终行为

### 3. 禁用 FileEasy 远程管理 API

FileEasy 模式下，远程管理 API 不应继续成功暴露：

- `GET /api/v1/files`
- `GET /api/v1/files/{id}`
- `GET /api/v1/files/{id}/preview`
- `GET /api/v1/files/{id}/download`
- `PATCH /api/v1/files/{id}`
- `DELETE /api/v1/files/{id}`
- `POST /api/v1/files/batch-delete`
- `/uploads/{path...}` 下载面

它们可以返回 `404`、`410 Gone` 或其他明确非成功响应，但不能继续作为 v1 可用能力。

### 4. 保留上传页能力

不要破坏：

- `GET /api/v1/ping`
- `GET /api/v1/home/summary`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/upload/init`
- `POST /api/v1/upload/chunk`
- `GET /api/v1/upload/status/{uploadId}`
- `DELETE /api/v1/upload/status/{uploadId}` 如果上传页仍使用取消
- `POST /api/v1/upload/complete`

### 5. 保持产品边界

不要引入：

- 播放器入口
- 监控面板
- 自动发现
- 公网访问
- 本机文件管理新功能
- 远程管理替代页面

## 最低验证要求

至少执行并报告：

```bash
npm --prefix apps/web-admin run build
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin
```

如果有真机，请继续执行并报告：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew installFileeasyDebug
curl -sS -m 5 -D - http://<device-ip>:3000/ | head
curl -sS -m 5 -D - http://<device-ip>:3000/admin | head
curl -sS -m 5 -D - http://<device-ip>:3000/api/v1/files | head
curl -sS -m 5 -D - http://<device-ip>:3000/uploads/nonexistent | head
```

真机验证时请说明：

- `<device-ip>` 来源
- `/` 是否仍打开上传页
- `/admin` 的最终行为
- `/api/v1/files` 的最终状态码
- APK 首页是否已无 admin 入口

如果没有真机，不要宣称发布 blocker 已关闭，只能写“代码侧已完成，等待 PM 真机复验”。

## 交付格式

1. Completed work
- 本次完成了什么

2. Changed behavior
- `/` 现在是什么行为
- `/admin` 现在是什么行为
- `/api/v1/files*` 和 `/uploads/*` 现在是什么行为

3. Changed files
- 改了哪些文件

4. Verification performed
- 做了哪些验证
- 哪些通过
- 哪些没法验证

5. Scope boundary
- 确认没有引入播放器、监控、自动发现、公网访问或替代管理页

6. Known risks
- 还剩哪些风险

7. PM handoff
- 是否建议 PM 复验
- 是否需要后续单独开 APK 本机文件管理任务
