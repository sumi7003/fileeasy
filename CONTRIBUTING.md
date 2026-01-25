# 贡献指南 (Contributing Guide)

感谢你对 Xplay 项目的关注。为了保证代码质量和项目的可持续发展，我们制定了以下开发规范。任何代码贡献必须遵循此文档。

## 1. 分支管理 (Branching Strategy)

我们采用简化版的 Git Flow：

- **`main`**: 生产环境分支，随时可部署，极其稳定。禁止直接 Push。
- **`develop`**: 开发主分支，包含最新的功能。
- **`feature/<name>`**: 功能分支，从 develop 切出，完成后合并回 develop。
- **`fix/<bug-name>`**: 修复分支。

**示例**:
```bash
git checkout -b feature/playlist-schedule develop
```

## 2. 提交规范 (Commit Convention)

我们使用 **Conventional Commits** 规范，提交信息格式如下：

```text
<type>(<scope>): <subject>
```

**Type 列表**:
- `feat`: 新功能 (Feature)
- `fix`: 修复 Bug
- `docs`: 文档变更
- `style`: 代码格式调整 (不影响逻辑)
- `refactor`: 代码重构 (无新功能，无 Bug 修复)
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

**示例**:
- ✅ `feat(server): add websocket heartbeat`
- ✅ `fix(android): resolve video loop crash`
- ❌ `update code` (拒绝此类模糊描述)

## 3. 代码规范 (Code Style)

项目配置了严格的 ESLint 和 Prettier。

- **Linting**: 代码提交前会自动运行 `lint-staged`。如果 lint 失败，提交将被阻止。
- **Formatting**: 不要手动调整格式，保存时让编辑器自动格式化，或运行 `pnpm format`。

## 4. 开发流程 (Workflow)

1. **领取任务**: 确认需求与设计文档。
2. **创建分支**: 命名符合规范。
3. **编写代码**:
   - 保持函数短小单一职责。
   - 关键逻辑必须编写单元测试 (Unit Test)。
   - **Any 类型零容忍**: 尽量避免使用 `any`，定义清晰的 Interface。
4. **提交代码**: 运行 `pnpm test` 确保无报错。
5. **发起 PR (Pull Request)**:
   - PR 标题清晰。
   - 描述变更内容和验证方式。
   - 指定至少一名 Reviewer。

## 5. Code Review 标准

Reviewer 在审核时需关注：
- **逻辑正确性**: 是否满足业务需求，有无边缘情况 bug。
- **代码可读性**: 命名是否规范，注释是否清晰。
- **安全性**: SQL 注入、权限校验等。
- **测试覆盖**: 核心业务逻辑是否有测试用例。

---
**质量是设计出来的，不是测试出来的。**

